/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/
package clus.algo.tdidt;

import clus.algo.split.FindBestTest;
import static clus.algo.tdidt.ClusNode.NO;
import static clus.algo.tdidt.ClusNode.UNK;
import static clus.algo.tdidt.ClusNode.YES;
import static clus.algo.tdidt.LookAheadPBCT.HORIZONTAL_SPLIT;
import static clus.algo.tdidt.LookAheadPBCT.VERTICAL_SPLIT;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.ClusSchema;
import clus.data.type.NumericAttrType;
import clus.main.ClusRun;
import clus.main.ClusStatManager;
import clus.model.ClusModel;
import clus.statistic.ClusStatistic;
import clus.statistic.RegressionStat;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import jeans.util.MyArray;
import jeans.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// ********************************
// PBCT-HMC
// author: @zamith
// ********************************
public class ClusNodePBCT implements ClusModel {
    protected ClusNode m_NodeVertical;
    protected ClusNode m_NodeHorizontal;
    protected ClusStatManager m_StatManager;
    protected ClusStatManager m_VerticalStatManager;
    protected ClusSchema m_Schema;
    protected ClusSchema m_VerticalSchema;
    protected RowData m_Data;
    protected RowData m_VerticalData;
    protected int m_TypeSplit;
    protected MyArray m_Children = new MyArray();
    protected ClusNodePBCT m_Parent;
    protected double m_Mean;
    protected int[] m_GlobalIndexes;
    protected boolean m_HasUnkBranch;
    protected FindBestTest m_FindBestTest;
    protected FindBestTest m_FindVerticalBestTest;
    public double m_FTest;
    
    public int m_ID;
    
    public ClusNodePBCT(){
        m_Children.setSize(2);
    }
    
    public ClusNodePBCT(ClusNode horizontal, ClusNode vertical, ClusStatManager stat, ClusStatManager verticalStat){
        m_Children.setSize(2);
        m_NodeVertical = vertical;
        m_NodeHorizontal = horizontal;
        m_StatManager = stat;
        m_VerticalStatManager = verticalStat;
        m_FindBestTest = new FindBestTest(m_StatManager);
        m_FindVerticalBestTest = new FindBestTest(m_VerticalStatManager);
    }
    
    public ClusNodePBCT(ClusNode horizontal, ClusNode vertical, ClusStatManager stat, ClusStatManager verticalStat, ClusSchema schema, ClusSchema verticalSchema){
        m_Children.setSize(2);
        m_NodeVertical = vertical;
        m_NodeHorizontal = horizontal;
        m_StatManager = stat;
        m_VerticalStatManager = verticalStat;
        m_Schema = schema;
        m_VerticalSchema = verticalSchema;
        m_FindBestTest = new FindBestTest(m_StatManager);
        m_FindVerticalBestTest = new FindBestTest(m_VerticalStatManager);
    }
    
    public ClusNodePBCT(ClusNode horizontal, ClusNode vertical, ClusStatManager stat, ClusStatManager verticalStat, ClusSchema schema, ClusSchema verticalSchema, RowData data, RowData verticalData){
        m_Children.setSize(2);
        m_NodeVertical = vertical;
        m_NodeHorizontal = horizontal;
        m_StatManager = stat;
        m_VerticalStatManager = verticalStat;
        m_Schema = schema;
        m_VerticalSchema = verticalSchema;
        m_Data = data;
        m_VerticalData = verticalData;
        m_FindBestTest = new FindBestTest(m_StatManager);
        m_FindVerticalBestTest = new FindBestTest(m_VerticalStatManager);
    }
    
    public ClusNode getNodeVertical(){
        return m_NodeVertical;
    }
    
    public ClusNode getNodeHorizontal(){
        return m_NodeHorizontal;
    }
    
    public ClusStatManager getStatManager(){
        return m_StatManager;
    }
    
    public ClusStatManager getVerticalStatManager(){
        return m_VerticalStatManager;
    }
    
    public ClusSchema getSchema(){
        return m_Schema;
    }
    
    public ClusSchema getVerticalSchema(){
        return m_VerticalSchema;
    }
    
    public RowData getData(){
        return m_Data;
    }
    
    public RowData getVerticalData(){
        return m_VerticalData;
    }
    
    public int getTypeSplit(){
        return m_TypeSplit;
    }
    
    public String getTypeSplitName(){
        if(m_TypeSplit == HORIZONTAL_SPLIT){
            return "Horizontal";
        }
        else if(m_TypeSplit == VERTICAL_SPLIT){
            return "Vertical";
        }
        else{
            return "Unknown Split";
        }
    }
    
    public FindBestTest getFindBestTest(){
        return m_FindBestTest;
    }
    
    public FindBestTest getVerticalFindBestTest(){
        return m_FindVerticalBestTest;
    }
    
    public void setNodeVertical(ClusNode node){
        m_NodeVertical = node;
    }
    
    public void setNodeHorizontal(ClusNode node){
        m_NodeHorizontal = node;
    }
    
    public void setStatManager(ClusStatManager stat){
        m_StatManager = stat;
    }
    
    public void setVerticalStatManager(ClusStatManager stat){
        m_VerticalStatManager = stat;
    }
    
    public void setSchema(ClusSchema schema){
        m_Schema = schema;
    }
    
    public void setVerticalSchema(ClusSchema schema){
        m_VerticalSchema = schema;
    }
    
    public void setData(RowData data){
        m_Data = data;
    }
    
    public void setVerticalData(RowData data){
        m_VerticalData = data;
    }
    
    public void setTypeSplit(int type){
        m_TypeSplit = type;
    }

    public ClusNodePBCT predictWeightedNode(DataTuple tupleHorizontal, DataTuple tupleVertical, int index) {
        if (atBottomLevel()) {           
            double weight = this.m_NodeHorizontal.getTargetStat().getSumWeights(getArrayIndex(this.getGlobalIndexes(),index));
            double value = this.m_NodeHorizontal.getTargetStat().getSumValues(getArrayIndex(this.getGlobalIndexes(),index));
            this.m_Mean = weight != 0.0 ? value / weight : 0.0;
            return this;
	} else {
            ClusNode node = getCorrespondingNode();
            DataTuple tuple = getCorrespondingTuple(tupleHorizontal,tupleVertical);
            int n_idx = node.m_Test.predictWeighted(tuple);
            //System.out.println("Type Split = "+this.m_TypeSplit+" Division = "+node.m_Test.getString()+" Lado = "+n_idx);
            if (n_idx != -1) {
                ClusNodePBCT info = this.getChild(n_idx);
                return info.predictWeightedNode(tupleHorizontal, tupleVertical, index);
            } else {
            	ClusNodePBCT ch0 = getChild(0).predictWeightedNode(tupleHorizontal, tupleVertical, index);
                ClusNodePBCT ch1 = getChild(1).predictWeightedNode(tupleHorizontal, tupleVertical, index);
		ClusNodePBCT res = new ClusNodePBCT();
                res.m_Mean=ch0.m_Mean*this.getCorrespondingNode().m_Test.getProportion(0);
                res.m_Mean+=ch1.m_Mean*this.getCorrespondingNode().m_Test.getProportion(1);
                res.m_GlobalIndexes=ch0.m_GlobalIndexes;
                res.m_HasUnkBranch=true;
		return this;
            }
	}
    }
    
    public ClusStatistic predictWeighted(DataTuple tuple, RowData verticalData) {
        int nbTarget = tuple.getSchema().getNbTargetAttributes();
        int nbTree = verticalData.getNbRows();
        NumericAttrType[] attrs = new NumericAttrType[nbTarget];
            
        for(int i=0; i<nbTree; i++){
            if(tuple.getSchema().getTargetAttributes()[i] instanceof NumericAttrType) {
                attrs[i]=(NumericAttrType) tuple.getSchema().getTargetAttributes()[i];
            }
        }
 
        ClusStatistic res = new RegressionStat(attrs, true, true);
            
        for(int i=0; i<nbTree; i++){
            if(!res.getFilled(i)) { 
                ClusNodePBCT exit = predictWeightedNode(tuple, verticalData.getTuple(i),i);
                if(!exit.m_HasUnkBranch){
                    res.includeElements(exit);
                    res.calcMean(exit);
                }
                else {
                    res.calcMean(exit.m_Mean, i);
                }   
                
            }
        }
        
        correctHierarchy((RegressionStat) res);
        return res;
    }
    
    

    @Override
    public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public int getModelSize() {
         return getNbNodes();
    }

    public String getModelInfo() {
         return "Nodes = "+getNbNodes()+" (Leaves: "+getNbLeaves()+")";
    }
    
    public final int getNbNodes() {
        int count = 1;
        int nb = getNbChildren();
        for (int i = 0; i < nb; i++) {
                ClusNodePBCT node = getChild(i);
                count += node.getNbNodes();
        }
        return count;
    }

	public final int getNbLeaves() {
            int nb = getNbChildren();
            if (nb == 0) {
                    return 1;
            } else {
                    int count = 0;
                    for (int i = 0; i < nb; i++) {
                            ClusNodePBCT node = getChild(i);
                            count += node.getNbLeaves();
                    }
                    return count;
            }
	}
   
    public final int getNbChildren() {
        return m_Children.size();
    }    
    
    public final void removeAllChildren() {
		int nb = getNbChildren();
		for (int i = 0; i < nb; i++) {
			ClusNodePBCT node = getChild(i);
			node.setParent(null);
		}
		m_Children.removeAllElements();
	}

    public void printModel(PrintWriter wrt) {
        printTree(wrt, StatisticPrintInfo.getInstance(), "");
    }

    public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
            printTree(wrt, info, "");
    }

    public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
            printTree(wrt, info, "", examples);
    }


    public final void printTree() {
            PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
            printTree(wrt, StatisticPrintInfo.getInstance(), "");
            wrt.flush();
    }

	public final void writeDistributionForInternalNode(PrintWriter writer, StatisticPrintInfo info) {
            if (info.INTERNAL_DISTR) {
                if (getCorrespondingNode().m_TargetStat != null) {
                        writer.print(": "+getCorrespondingNode().m_TargetStat.getString(info));
                }
            }
            writer.println();
	}

	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix) {
		printTree( writer,  info,  prefix, null);
	}

	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix, RowData examples) {
		int arity = getNbChildren();
		if (arity > 0) {			
			int delta = getCorrespondingNode().hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.print(getCorrespondingNode().m_Test.getTestString());
				RowData examples0 = null;
				RowData examples1 = null;
				if (examples!=null){
					examples0 = examples.apply(getCorrespondingNode().m_Test, 0);
					examples1 = examples.apply(getCorrespondingNode().m_Test, 1);
				}				
				writeDistributionForInternalNode(writer, info);
				writer.print(prefix + "+--yes: ");
				((ClusNodePBCT)getChild(YES)).printTree(writer, info, prefix+"|       ",examples0);
				writer.print(prefix + "+--no:  ");
				if (getCorrespondingNode().hasUnknownBranch()) {
					((ClusNodePBCT)getChild(NO)).printTree(writer, info, prefix+"|       ",examples1);
					writer.print(prefix + "+--unk: ");
					((ClusNodePBCT)getChild(UNK)).printTree(writer, info, prefix+"        ",examples0);
				} else {
					((ClusNodePBCT)getChild(NO)).printTree(writer, info, prefix+"        ",examples1);
				}
			} else {				
				writer.println(getCorrespondingNode().m_Test.getTestString());				
				for (int i = 0; i < arity; i++) {
					ClusNodePBCT child = (ClusNodePBCT)getChild(i);
					String branchlabel = getCorrespondingNode().m_Test.getBranchLabel(i);
					RowData examplesi = null;
					if (examples!=null){
						examples.apply(getCorrespondingNode().m_Test, i);
					}
					writer.print(prefix + "+--" + branchlabel + ": ");
					String suffix = StringUtils.makeString(' ', branchlabel.length()+4);
					if (i != arity-1) {
						child.printTree(writer, info, prefix+"|"+suffix,examplesi);
					} else {
						child.printTree(writer, info, prefix+" "+suffix,examplesi);
					}
				}
			}
		} else {//on the leaves
			if (getCorrespondingNode().m_TargetStat == null) {
				writer.print("?");
			} else {				
				writer.print(getCorrespondingNode().m_TargetStat.getString(info));				
			}
			//if (getID() != 0 && info.SHOW_INDEX) writer.println(" ("+getID()+")");
			//else writer.println();
                        writer.println();
			if (examples!=null && examples.getNbRows()>0){
				writer.println(examples.toString(prefix));
				writer.println(prefix+"Summary:");
				writer.println(examples.getSummary(prefix));
			}

		}
	}

	public String printTestNode(String a, int pres){
		if(pres == 1) {return a;}
		else {return ("not("+a+")");}
	}

	public String toString() {
		try{
			if (m_NodeHorizontal.hasBestTest()) return m_NodeHorizontal.getTestString();
			else return m_NodeHorizontal.m_TargetStat.getSimpleString();
		}
		catch(Exception e){return "null clusnode ";}
	}
    @Override
    public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem, boolean exhaustive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void printModelToPythonScript(PrintWriter wrt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element printModelToXML(Document doc, StatisticPrintInfo info, RowData examples) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void attachModel(HashMap table) throws ClusException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void retrieveStatistics(ArrayList list) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ClusModel prune(int prunetype) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getID() {
        return m_ID;
    }
    
    protected final boolean atBottomLevel() {
	return ((m_NodeHorizontal.getNbChildren() == 0)&&(m_NodeVertical.getNbChildren() == 0));
    }
    
    protected final ClusNode getCorrespondingNode() {
        if(m_TypeSplit == VERTICAL_SPLIT){
            return m_NodeVertical;
        }
        else{
            return m_NodeHorizontal;
        }
    }
    
    protected final DataTuple getCorrespondingTuple(DataTuple tupleHorizontal, DataTuple tupleVertical) {
        if(m_TypeSplit == VERTICAL_SPLIT){
            return tupleVertical;
        }
        else{
            return tupleHorizontal;
        }
    }
    
    public final ClusNodePBCT getChild(int idx) {
	return (ClusNodePBCT)m_Children.elementAt(idx);
    }
    
    public final void setChild(ClusNodePBCT node, int idx) {
	node.setParent(this);
	m_Children.setElementAt(node, idx);
    }
    
    public final ClusNodePBCT getParent() {
	return m_Parent;
    }

    public final void setParent(ClusNodePBCT parent) {
	m_Parent = parent;
    }
    
    public int[] getGlobalIndexes(){
            return m_GlobalIndexes;
    }
    
    public void initializeGlobalIndexes(int size){
            m_GlobalIndexes = new int[size];
            for(int i=0; i<size; i++){
                m_GlobalIndexes[i] = i;
            } 
        
    }
    
    public void infNode(int split, int[] indexesYes) {
        if(split == VERTICAL_SPLIT) m_GlobalIndexes=getGlobal(getParent().getGlobalIndexes(),indexesYes);
        if(split == HORIZONTAL_SPLIT) m_GlobalIndexes=getParent().getGlobalIndexes().clone();
     }
    
    public int[] getGlobal(int[] parentGlobalIndexes, int[] indexesYes) {
            int[] indexesGlobal = new int[indexesYes.length];
            for(int i = 0; i<indexesYes.length; i++){
                indexesGlobal[i]=parentGlobalIndexes[indexesYes[i]];
            }
            return indexesGlobal;
    }
    
    public int getArrayIndex(int[] arr,int value) {
            int k=-1;
            for(int i=0;i<arr.length;i++){
                if(arr[i]==value){
                    k=i;
                    break;
                }
            }
            return k;
        } 

    @Override
    public ClusStatistic predictWeighted(DataTuple tuple) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean hasParent(){
        return m_Parent != null;
    }
    
    public ClusNodePBCT cloneNode() throws ClusException, IOException{
        ClusNodePBCT res = new ClusNodePBCT();
        res.m_Data = (RowData) this.m_Data.cloneData();
        res.m_VerticalData = (RowData) this.m_VerticalData.cloneData();
        res.m_Schema = this.m_Schema;
        res.m_VerticalSchema = this.m_VerticalSchema;
        res.m_StatManager = this.m_StatManager;
        res.m_VerticalStatManager = this.m_VerticalStatManager;
        /*res.m_StatManager = new ClusStatManager(res.m_Schema,res.getSchema().getSettings());
        res.m_StatManager.initStatisticAndStatManager();
	res.m_StatManager.initHeuristic();
        res.m_StatManager.initStopCriterion();
        res.m_VerticalStatManager = new ClusStatManager(res.m_VerticalSchema,res.getVerticalSchema().getSettings());
	res.m_VerticalStatManager.initHeuristic();
        res.m_VerticalStatManager.initStopCriterion();
        res.m_VerticalStatManager.initStatisticAndStatManager();*/
        res.m_FindBestTest = new FindBestTest(m_StatManager);
        res.m_FindVerticalBestTest = new FindBestTest(m_VerticalStatManager);
        res.m_NodeHorizontal = this.m_NodeHorizontal;
        res.m_NodeVertical = this.m_NodeVertical;
        
        return res;
    }
    
    public void correctHierarchy(RegressionStat pred){
        String hSeparator = this.m_Schema.getSettings().getHierSep();
        for(int i=0; i<pred.m_NbAttrs; i++){
                String className = pred.m_Attrs[i].getName()+hSeparator;
                for(int j=i+1; j<pred.m_NbAttrs; j++){
                   String anotherClass = pred.m_Attrs[j].getName()+hSeparator;
                   if((className.length()<anotherClass.length())&&(className.equals(anotherClass.substring(0,className.length())))){
                       if(pred.getMean(j)>pred.getMean(i)){
                           pred.m_SumWeights[j]=pred.m_SumWeights[i];
                           pred.m_Means[j]=pred.m_Means[i];
                       }
                   }
                   else{
                       break;
                   }
                }
                
        }
    }
    
}
