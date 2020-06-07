package clus.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import clus.data.attweights.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.hierarchical.ClassHierarchy;
import clus.ext.hierarchical.ClassTerm;
import clus.ext.hierarchical.ClassesTuple;
import clus.ext.hierarchical.ClassesValue;
import clus.ext.hierarchical.WHTDStatistic;
import clus.main.*;
import clus.statistic.*;
import clus.util.ClusFormat;

public class MultiLabelError extends ClusNumericError {
	
	protected int m_Compatibility;
	protected BinaryPredictionList[] m_ClassWisePredictions;
	protected ROCAndPRCurve[] m_ROCAndPRCurves;
	
	protected double m_AverageAUROC;
	protected double m_AverageAUPRC;
	protected double m_WAvgAUPRC;
	protected double m_PooledAUPRC;

        //PBCT
        public final static String[] HIERMEASURES = { "AverageAUROC", "AverageAUPRC", "WeightedAverageAUPRC", "PooledAUPRC" };

        public final static int HIERMEASURE_AUROC  = 0;
        public final static int HIERMEASURE_AUPRC  = 1;
        public final static int HIERMEASURE_WEIGHTED_AUPRC = 2;
        public final static int HIERMEASURE_POOLED_AUPRC = 3;
        

	public MultiLabelError(ClusErrorList par, NumericAttrType[] num, int compat) {
		super(par, num);
		m_Compatibility = compat;
		m_ClassWisePredictions = new BinaryPredictionList[num.length];
		m_ROCAndPRCurves = new ROCAndPRCurve[num.length];
		for (int i = 0; i < num.length; i++) {
			BinaryPredictionList predlist = new BinaryPredictionList();
			m_ClassWisePredictions[i] = predlist;
			m_ROCAndPRCurves[i] = new ROCAndPRCurve(predlist);
		}
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double[] predicted = pred.getNumericPred();
		for (int i = 0; i < m_Dim; i++) {
			double actualdouble = getAttr(i).getNumeric(tuple);
			//actualdouble = 0 or 1
			boolean actual;
			if (actualdouble > 0.5) actual = true;
			else actual = false;
			m_ClassWisePredictions[i].addExample(actual, predicted[i]);
		}
	}
	
	public void addExample(double[] real, double[] predicted) {
		for (int i = 0; i < m_Dim; i++) {
			if (real[i] > 0.5) m_ClassWisePredictions[i].addExample(true, predicted[i]);
			else m_ClassWisePredictions[i].addExample(false, predicted[i]);
		}	
	}
	
	public double getModelError() {
		computeAll();
		return m_AverageAUPRC;
	}
	
	public void compatibility(ROCAndPRCurve[] curves, ROCAndPRCurve pooled) {
		double[] thr = null;
		if (m_Compatibility <= Settings.COMPATIBILITY_MLJ08) {
			thr = new double[51];
			for (int i = 0; i <= 50; i++) {
				thr[i] = (double)2*i/100.0;
			}
		}
		for (int i = 0; i < curves.length; i++) {
			curves[i].setThresholds(thr);
		}
		pooled.setThresholds(thr);
	}
        
        // ********************************
        //PBCT: To be similar to HierErrorMeasures
        public boolean isEvalClass(int idx) {
		// Don't include trivial classes (with only pos or only neg examples)
		return includeZeroFreqClasses(idx); // && m_ClassWisePredictions[idx].hasBothPosAndNegEx();
	}
        // ********************************

	public boolean includeZeroFreqClasses(int idx) {
		// Averages never include classes with zero frequency in test set
		return m_ClassWisePredictions[idx].getNbPos() > 0;
	}

        // ********************************
        //PBCT: Included EvalClass method
	public void computeAll() {
		BinaryPredictionList pooled = new BinaryPredictionList();
		ROCAndPRCurve pooledCurve = new ROCAndPRCurve(pooled);
		compatibility(m_ROCAndPRCurves, pooledCurve);
		for (int i = 0; i < m_Dim; i++) {
                    if(isEvalClass(i)){
				m_ClassWisePredictions[i].sort();
				m_ROCAndPRCurves[i].computeCurves();
				m_ROCAndPRCurves[i].clear();
				pooled.add(m_ClassWisePredictions[i]);
				m_ClassWisePredictions[i].clearData();
                    }
		}
		pooled.sort();
		pooledCurve.computeCurves();
		pooledCurve.clear();
		// Compute averages
		int cnt = 0;
		double sumAUROC = 0.0;
		double sumAUPRC = 0.0;
		double sumAUPRCw = 0.0;
		double sumFrequency = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			// In compatibility mode, averages never include classes with zero frequency in test set
                        if(isEvalClass(i)){
                                double freq = m_ClassWisePredictions[i].getFrequency();
				sumAUROC += m_ROCAndPRCurves[i].getAreaROC();
				sumAUPRC += m_ROCAndPRCurves[i].getAreaPR();
				sumAUPRCw += freq*m_ROCAndPRCurves[i].getAreaPR();
				sumFrequency += freq;
				cnt++;
                        }
		}   		
		m_AverageAUROC = sumAUROC / cnt;
		m_AverageAUPRC = sumAUPRC / cnt;
		m_WAvgAUPRC = sumAUPRCw / sumFrequency;
		m_PooledAUPRC = pooledCurve.getAreaPR();
	}
	// ********************************

	public void showModelError(PrintWriter out, String bName, int detail) throws IOException {
		
		NumberFormat fr1 = ClusFormat.SIX_AFTER_DOT;
		computeAll();
		out.println();
		out.println("      Average AUROC:            "+m_AverageAUROC);
		out.println("      Average AUPRC:            "+m_AverageAUPRC);
		out.println("      Average AUPRC (weighted): "+m_WAvgAUPRC);
		out.println("      Pooled AUPRC:             "+m_PooledAUPRC);

		if (detail != ClusError.DETAIL_VERY_SMALL) {
			printResults(fr1, out);
		}

	}
	
	public String showModelError(String bName, int detail) throws IOException {
		
		String out="\n";
		NumberFormat fr1 = ClusFormat.SIX_AFTER_DOT;
		computeAll();
	
		out+="      Average AUROC:            "+m_AverageAUROC+"\n";
			
		out+="      Average AUPRC:            "+m_AverageAUPRC+"\n";
		out+="      Average AUPRC (weighted): "+m_WAvgAUPRC+"\n";
		out+="      Pooled AUPRC:             "+m_PooledAUPRC+"\n";

		if (detail != ClusError.DETAIL_VERY_SMALL) {
			out+=printResults(fr1);
		}
		
		return out;

	}
	

	public void printResults(NumberFormat fr, PrintWriter out) {
		for (int i = 0; i < m_Dim; i++) {
			out.print("      "+ i + ": " + m_Attrs[i].getName()+": ");
			out.print(" AUROC: "+fr.format(m_ROCAndPRCurves[i].getAreaROC()));
			out.print(", AUPRC: "+fr.format(m_ROCAndPRCurves[i].getAreaPR()));
			out.print(", Freq: "+fr.format(m_ClassWisePredictions[i].getFrequency()));
			out.println();
		}
	}
	
	public String printResults(NumberFormat fr) {
		String out="";
		for (int i = 0; i < m_Dim; i++) {
			out+="      "+ i + ": " + m_Attrs[i].getName()+": ";
			out+=" AUROC: "+fr.format(m_ROCAndPRCurves[i].getAreaROC());
			out+=", AUPRC: "+fr.format(m_ROCAndPRCurves[i].getAreaPR());
			out+=", Freq: "+fr.format(m_ClassWisePredictions[i].getFrequency());
			out+="\n";
		}
		return out;
	}
	
	public ClusError getErrorClone(ClusErrorList par) {
		return new MultiLabelError(par, m_Attrs, m_Compatibility);
	}
	
	public String getName() {
		return "Multi-label error measures";
	}
        
        //PBCT
        public void add(ClusError other) {
            BinaryPredictionList[] olist = ((MultiLabelError)other).m_ClassWisePredictions;
            for (int i = 0; i < m_Dim; i++) {
                    m_ClassWisePredictions[i].add(olist[i]);
            }
	}
        
        public double getModelError(int i) {
            computeAll();
            if(i == HIERMEASURE_AUPRC)
                return m_AverageAUPRC;
            if(i == HIERMEASURE_AUROC)
                return m_AverageAUROC;
            if(i == HIERMEASURE_POOLED_AUPRC)
                return m_PooledAUPRC;
            if(i == HIERMEASURE_WEIGHTED_AUPRC)
                return m_WAvgAUPRC;
            return 0.0;
	}
        
        public String getName(int i){
            return HIERMEASURES[i];
        }
}
