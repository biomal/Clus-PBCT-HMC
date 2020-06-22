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

import static clus.Clus.HORIZONTAL_DATA;
import static clus.Clus.VERTICAL_DATA;

import clus.main.*;
import clus.util.*;
import clus.algo.*;
import clus.algo.split.*;
import clus.data.ClusData;
import clus.data.rows.*;
import clus.data.type.*;
import clus.model.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.heuristic.*;

import java.io.*;
import java.util.*;
import jeans.util.IntervalCollection;

// ********************************
// PBCT-HMC
// author: @zamith
// ********************************
public class LookAheadPBCT extends ClusInductionAlgorithm {

        public final static int HORIZONTAL_SPLIT = 1;
        public final static int VERTICAL_SPLIT = 2;
    
	protected ClusNodePBCT m_Root;        
        protected String[] m_NamesAttrs;

	public LookAheadPBCT(ClusSchema schema, ClusSchema verticalSchema, Settings sett) throws ClusException, IOException {
		super(schema, verticalSchema, sett);
	}

	public LookAheadPBCT(ClusInductionAlgorithm other) {
		super(other);
	}

	public void initialize() throws ClusException, IOException {
		m_StatManager.initStatisticAndStatManager();
                m_VerticalStatManager.initStatisticAndStatManager();
	}
        
        public boolean initSelectorAndStopCrit(ClusNodePBCT node, int type) {
		int max = getSettings().getTreeMaxDepth();
		if(type==HORIZONTAL_DATA){
                    if (max != -1 && node.getNodeHorizontal().getLevel() >= max) return true;
                    return node.getFindBestTest().initSelectorAndStopCrit(node.getNodeHorizontal().getClusteringStat(), node.getData());
                }
                else{
                    if (max != -1 && node.getNodeVertical().getLevel() >= max) return true;
                    return node.getVerticalFindBestTest().initSelectorAndStopCrit(node.getNodeVertical().getClusteringStat(), node.getVerticalData());
                }
	}

	public ClusAttrType[] getDescriptiveAttributes(ClusNodePBCT node, int type) {
		ClusSchema schema = null;
                if(type == HORIZONTAL_DATA) schema = node.getSchema();
                else schema = node.getVerticalSchema();
		Settings sett = getSettings();
                return schema.getDescriptiveAttributes();
	}

	public void makeLeaf(ClusNode node) {
		node.makeLeaf();
	}

        public void induce(ClusNodePBCT node) throws ClusException, IOException {
		//System.out.println("nonsparse induce");
		// Initialize selector and perform various stopping criteria
                                
                ClusNode horizontal = node.getNodeHorizontal();
                ClusNode vertical = node.getNodeVertical();
                
                getSettings().setFTest(node.m_FTest);
                
                initSelectorAndSplit(node, horizontal.getClusteringStat(),HORIZONTAL_DATA);
                initSelectorAndSplit(node, vertical.getClusteringStat(),VERTICAL_DATA);
                
		if (initSelectorAndStopCrit(node, HORIZONTAL_DATA)) {
			makeLeaf(horizontal);
                        makeLeaf(vertical);
                        node.removeAllChildren();
			return;
		}
                
                
                if (initSelectorAndStopCrit(node, VERTICAL_DATA)) {
			makeLeaf(horizontal);
                        makeLeaf(vertical);
                        node.removeAllChildren();
			return;
		}
                
		// Find best test
		
//		long start_time = System.currentTimeMillis();
		// Find best horizontal
		ClusAttrType[] attrs = getDescriptiveAttributes(node,HORIZONTAL_DATA);
		for (int i = 0; i < attrs.length; i++) {
			ClusAttrType at = attrs[i];
			if (at instanceof NominalAttrType) node.getFindBestTest().findNominal((NominalAttrType)at, node.getData());
			else node.getFindBestTest().findNumeric((NumericAttrType)at, node.getData());
                        
		}
                
                attrs = getDescriptiveAttributes(node,VERTICAL_DATA);
                double bestVertical = Double.NEGATIVE_INFINITY;
                ClusAttrType bestAttrVertical = null;
                if(!(node.hasParent() && node.getParent().m_TypeSplit == VERTICAL_SPLIT)){
                    for (int i = 0; i < attrs.length; i++) {
                            ClusAttrType at = attrs[i];
                            node.getVerticalFindBestTest().m_BestTest.resetBestTest();
                            node.getVerticalFindBestTest().findNumeric((NumericAttrType)at, node.getVerticalData());
                            if (node.getVerticalFindBestTest().getBestTest().hasBestTest()){
                                double heur = induceLookAhead(node);
                                if(heur>=bestVertical){
                                    bestVertical=heur;
                                    bestAttrVertical=at;
                                }
                            }
                    }
                }
                if(bestAttrVertical!=null){
                    node.getVerticalFindBestTest().m_BestTest.resetBestTest();
                    node.getVerticalFindBestTest().findNumeric((NumericAttrType)bestAttrVertical, node.getVerticalData());
                }
                
                getSettings().setFTest(node.m_FTest);
		
		// Partition data + recursive calls
                CurrentBestTestAndHeuristic best = node.getFindBestTest().getBestTest();
                node.setTypeSplit(HORIZONTAL_SPLIT);
		if(bestVertical>node.getFindBestTest().getBestTest().getHeuristicValue()){
                    best = node.getVerticalFindBestTest().getBestTest();
                    node.setTypeSplit(VERTICAL_SPLIT);
                }
                
                if (best.hasBestTest()) {
//			start_time = System.currentTimeMillis();
			if(node.getTypeSplit()==HORIZONTAL_SPLIT){
                            horizontal.testToNode(best);

                            // Output best test
                            if (Settings.VERBOSE > 0) System.out.println("Test: "+horizontal.getTestString()+" -> "+best.getHeuristicValue()+" ("+node.getTypeSplitName()+")");
                            // Create children
                            int arity = horizontal.updateArity();
                            //Same for Vertical
                            vertical.setNbChildren(arity);

                            NodeTest test = horizontal.getTest();
                            RowData[] subsets = new RowData[arity];
                            int[][] indexes = new int[arity][];
                            for (int j = 0; j < arity; j++) {
                                    subsets[j] = node.getData().applyWeighted(test, j);
                                    indexes[j] = node.getData().getIndexes(test,j);
                            }

                            for (int j = 0; j < arity; j++) {
                                    //Horizontal
                                    ClusNode childHorizontal = new ClusNode();
                                    horizontal.setChild(childHorizontal, j);
                                    childHorizontal.initClusteringStat(node.getStatManager(), horizontal.getClusteringStat(), subsets[j]);
                                    childHorizontal.initTargetStat(node.getStatManager(), horizontal.getTargetStat(), subsets[j]);

                                    node.getStatManager().initClusteringWeights(getNameAttributes(node.getSchema()));

                                    //Vertical
                                    ClusNode childVertical = new ClusNode();
                                    vertical.setChild(childVertical, j);

                                    ClusSchema newVerticalSchema = newSchema(node.getVerticalSchema(),indexes[j]);
                                    RowData newVerticalData = newData(node.getVerticalData(),newVerticalSchema,indexes[j]);
                                    ClusStatManager newVerticalStatManager = new ClusStatManager(newVerticalSchema,node.getVerticalSchema().getSettings());
                                    newVerticalStatManager.initStatisticAndStatManager();

                                    childVertical.initClusteringStat(newVerticalStatManager, newVerticalData);
                                    childVertical.initTargetStat(newVerticalStatManager, newVerticalData);

                                    initializeAttributeWeights(newVerticalStatManager);

                                    ClusNodePBCT newPBCTNode = new ClusNodePBCT(childHorizontal, childVertical, node.getStatManager(), newVerticalStatManager, node.getSchema(), newVerticalSchema, subsets[j], newVerticalData);  
                                    node.setChild(newPBCTNode,j);
                                    newPBCTNode.infNode(HORIZONTAL_SPLIT, indexes[j]);
                                    double factor = (double)(subsets[j].getNbRows())/(double)(subsets[0].getNbRows()+subsets[1].getNbRows());
                                    newPBCTNode.m_FTest = (node.m_FTest*factor);
                                                                        
                                    induce(newPBCTNode);
                                }
                        } else if(node.getTypeSplit()==VERTICAL_SPLIT){
                            vertical.testToNode(best);

                            // Output best test
                            if (Settings.VERBOSE > 0) System.out.println("Test: "+vertical.getTestString()+" -> "+best.getHeuristicValue()+" ("+node.getTypeSplitName()+")");
                            // Create children
                            int arity = vertical.updateArity();
                            //Same for Vertical
                            horizontal.setNbChildren(arity);

                            NodeTest test = vertical.getTest();
                            RowData[] subsets = new RowData[arity];
                            int[][] indexes = new int[arity][];
                            for (int j = 0; j < arity; j++) {
                                    subsets[j] = node.getVerticalData().applyWeighted(test, j);
                                    indexes[j] = node.getVerticalData().getIndexes(test,j);
                            }

                            for (int j = 0; j < arity; j++) {
                                    //Vertical
                                    ClusNode childVertical = new ClusNode();
                                    vertical.setChild(childVertical, j);
                                    childVertical.initClusteringStat(node.getVerticalStatManager(), vertical.getClusteringStat(), subsets[j]);
                                    childVertical.initTargetStat(node.getVerticalStatManager(), vertical.getTargetStat(), subsets[j]);

                                    //Horizontal
                                    ClusNode childHorizontal = new ClusNode();
                                    horizontal.setChild(childHorizontal, j);

                                    ClusSchema newSchema = newSchema(node.getSchema(),indexes[j]);
                                    RowData newData = newData(node.getData(),newSchema,indexes[j]);
                                    ClusStatManager newStatManager = new ClusStatManager(newSchema,node.getSchema().getSettings());
                                    newStatManager.initStatisticAndStatManager();

                                    childHorizontal.initClusteringStat(newStatManager, newData);
                                    childHorizontal.initTargetStat(newStatManager, newData);

                                    initializeAttributeWeights(newStatManager);
                                    
                                    node.getStatManager().initClusteringWeights(getNameAttributes(node.getSchema()));

                                    ClusNodePBCT newPBCTNode = new ClusNodePBCT(childHorizontal, childVertical, newStatManager, node.getVerticalStatManager(), newSchema, node.getVerticalSchema(), newData, subsets[j]);
                                    
                                    node.setChild(newPBCTNode,j);
                                    newPBCTNode.infNode(VERTICAL_SPLIT, indexes[j]);
                                    double factor = (double)(subsets[j].getNbRows())/(double)(subsets[0].getNbRows()+subsets[1].getNbRows());
                                    newPBCTNode.m_FTest = (node.m_FTest*factor);
                                    
                                    induce(newPBCTNode);
                            }
                    }
		} else {
			makeLeaf(horizontal);
                        makeLeaf(vertical);
                        node.removeAllChildren();
		}
	}
        
	public void initSelectorAndSplit(ClusNodePBCT node, ClusStatistic stat, int type) throws ClusException {
            if(type==HORIZONTAL_DATA) node.getFindBestTest().initSelectorAndSplit(stat);
            else node.getVerticalFindBestTest().initSelectorAndSplit(stat);
	}

	public void setInitialData(ClusNodePBCT node, ClusStatistic stat, RowData data, int type) throws ClusException {
            if(type==HORIZONTAL_DATA) node.getFindBestTest().setInitialData(stat,data);
            else node.getVerticalFindBestTest().setInitialData(stat,data);
	}

	public void cleanSplit(ClusNodePBCT node, int type) {
                if(type==HORIZONTAL_DATA) node.getFindBestTest().cleanSplit();
                else node.getVerticalFindBestTest().cleanSplit();
	}

        public ClusNodePBCT induceSingleUnpruned(RowData data, RowData verticalData) throws ClusException, IOException {
                m_Root = null;

		// Begin of induction process
		int nbr = 0;
		while (true) {
			nbr++;
			// Init root node
			ClusNode horizontal = new ClusNode();
                        ClusNode vertical = new ClusNode();
                        
			horizontal.initClusteringStat(m_StatManager, data);
			horizontal.initTargetStat(m_StatManager, data);
			horizontal.getClusteringStat().showRootInfo();
			
                        vertical.initClusteringStat(m_VerticalStatManager, verticalData);
			vertical.initTargetStat(m_VerticalStatManager, verticalData);
			vertical.getClusteringStat().showRootInfo();
                        
                        setNameAttributes();
                        m_StatManager.initClusteringWeights(getNameAttributes());
                        
                        m_Root = new ClusNodePBCT(horizontal, vertical, m_StatManager, m_VerticalStatManager, m_Schema, m_VerticalSchema, data, verticalData);
                        
                        m_Root.initializeGlobalIndexes(verticalData.getNbRows());
                        
                        initSelectorAndSplit(m_Root, horizontal.getClusteringStat(),HORIZONTAL_DATA);
			setInitialData(m_Root, horizontal.getClusteringStat(),data,HORIZONTAL_DATA);
                        
                        initSelectorAndSplit(m_Root, vertical.getClusteringStat(),VERTICAL_DATA);
			setInitialData(m_Root, vertical.getClusteringStat(),verticalData,VERTICAL_DATA);
                        
                        m_Root.m_FTest = getSettings().getFTest();
                        
			// Induce the tree
			induce(m_Root);
                        
			// rankFeatures(m_Root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}
		m_Root.getNodeHorizontal().postProc(null);

		cleanSplit(m_Root,HORIZONTAL_DATA);
                cleanSplit(m_Root,VERTICAL_DATA);
		return m_Root;
	}
        

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		return induceSingleUnpruned((RowData)cr.getTrainingSet(),(RowData)cr.getVerticalTrainingSet());
	}
        
        public void setNameAttributes(){
            NumericAttrType[] attrs = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
            m_NamesAttrs = new String[attrs.length];
            for(int i = 0; i<attrs.length;i++){
                m_NamesAttrs[i]=attrs[i].getName();
            }
        }
        
        public String[] getNameAttributes(ClusSchema schema){
            NumericAttrType[] attrs = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
            String[] names = new String[attrs.length];
            for(int i = 0; i<attrs.length;i++){
                names[i]=attrs[i].getName();
            }
            
            return names;
        }
        
        public String[] getNameAttributes(){
            return m_NamesAttrs;
        }
        
        public ClusNode getRootTree(){
            return m_Root.getNodeHorizontal();
        }
        
        public RowData newData(RowData oldData, ClusSchema newSchema, int[] indexes)
			throws IOException, ClusException {
            RowData res = null;
            int nbDescriptive = oldData.getSchema().getDescriptive().getMaxIndex();
            int[] indexesNot = new int[oldData.getSchema().getNbTargetAttributes()-indexes.length];
            
            int k=0;
            for(int i=0; i<oldData.getSchema().getNbTargetAttributes();i++) {
                boolean find = false;
                for(int j=0; j<indexes.length; j++){
                    if(indexes[j]==i){
                        find = true;
                        break;
                    }
                }
                if(!find){
                    indexesNot[k]=i;
                    k++;
                }
            }          
            for(int i = indexesNot.length -1; i>=0; i--){
                newSchema.removeAttrType(nbDescriptive+indexesNot[i]);
            }
                       
            String finalInterval = newSchema.getTarget().getMinIndex()+"-"+(newSchema.getTarget().getMaxIndex()-indexesNot.length);
            IntervalCollection interval = new IntervalCollection(finalInterval);
            newSchema.setTarget(interval);
            newSchema.setClustering(interval);
            newSchema.newInitialize();
            res = new RowData(newSchema);
            res.resize(oldData.getNbRows());
            
            int nNominal = 0;
            int nNumeric = 0;
            //descriptive
            for(int j = 0;j<oldData.getNbRows();j++){
                nNominal = 0;
                nNumeric = 0;
                for(int i = 0;i<nbDescriptive;i++){
                    ClusAttrType atType = newSchema.getAttrType(i);
                    //System.out.println(atType.getIndex());
                    if(atType instanceof NominalAttrType){ 
                        res.getData()[j].setIntVal(atType.getNominal(oldData.getTuple(j)), atType.getIndex()-nNumeric);     
                        nNominal++;
                    }
                    else {
                        res.getData()[j].setDoubleVal(atType.getNumeric(oldData.getTuple(j)), atType.getIndex()-nNominal);
                        nNumeric++;
                    }
                }
            }
            
            int tempNominal = nNominal;
            int tempNumeric = nNumeric;
            for(int j = 0;j<oldData.getNbRows();j++){
                nNominal = tempNominal;
                nNumeric = tempNumeric;
                for(int i = 0;i<indexes.length;i++){
                    ClusAttrType atType = newSchema.getAttrType(nbDescriptive+i);
                    if(atType instanceof NominalAttrType){ 
                        res.getData()[j].setIntVal(oldData.getSchema().getAttrType(nbDescriptive+indexes[i]).getNominal(oldData.getTuple(j)), atType.getIndex()-nNumeric);
                        nNominal++;
                    }
                    else {
                        res.getData()[j].setDoubleVal(oldData.getSchema().getAttrType(nbDescriptive+indexes[i]).getNumeric(oldData.getTuple(j)), atType.getIndex()-nNominal);
                        nNumeric++;
                    }
                }
            }    
            
            
            return res;
	} 
        
        public ClusSchema newSchema(ClusSchema oldSchema, int[] indexes) throws ClusException, IOException{
            ClusSchema newSchema = oldSchema.cloneSchema();
            newSchema.copyFrom(oldSchema);
            return newSchema;
        }
        
        public final void initializeAttributeWeights(ClusStatManager mgr) throws IOException, ClusException {
		ClusStatistic allStat = mgr.createStatistic(ClusAttrType.ATTR_USE_ALL);
		ClusStatistic[] stats = new ClusStatistic[1];
		stats[0] = allStat;
		/*
		 * if (!m_Sett.isNullTestFile()) { System.out.println("Loading: " +
		 * m_Sett.getTestFile()); updateStatistic(m_Sett.getTestFile(), stats);
		 * } if (!m_Sett.isNullPruneFile()) { System.out.println("Loading: " +
		 * m_Sett.getPruneFile()); updateStatistic(m_Sett.getPruneFile(),
		 * stats); }
		 */
		//mgr.initNormalizationWeights(allStat, data);
                mgr.initDispersionWeights();
		mgr.initHeuristic();
                mgr.initStopCriterion();
		mgr.initSignifcanceTestingTable();
                mgr.initClusteringWeights();
        }
        
        public final double induceLookAhead(ClusNodePBCT node) throws ClusException, IOException{
            CurrentBestTestAndHeuristic best = node.getVerticalFindBestTest().getBestTest();
            ClusNode vertical = node.getNodeVertical();
            ClusNode horizontal = node.getNodeHorizontal();
            vertical.testToNode(best);
            int arity = vertical.updateArity();
            horizontal.setNbChildren(arity);

            NodeTest test = vertical.getTest();
            RowData[] subsets = new RowData[arity];
            int[][] indexes = new int[arity][];
            for (int j = 0; j < arity; j++) {
                    subsets[j] = node.getVerticalData().applyWeighted(test, j);
                    indexes[j] = node.getVerticalData().getIndexes(test,j);
            }
            
            double heur[] = new double[arity];
            ClusNodePBCT nodes[] = new ClusNodePBCT[arity];
            for (int j = 0; j < arity; j++) {
                    //Vertical
                    ClusNode childVertical = new ClusNode();
                    vertical.setChild(childVertical, j);
                    childVertical.initClusteringStat(node.getVerticalStatManager(), vertical.getClusteringStat(), subsets[j]);
                    childVertical.initTargetStat(node.getVerticalStatManager(), vertical.getTargetStat(), subsets[j]);

                    //Horizontal
                    ClusNode childHorizontal = new ClusNode();
                    horizontal.setChild(childHorizontal, j);

                    ClusSchema newSchema = newSchema(node.getSchema(),indexes[j]);
                    RowData newData = newData(node.getData(),newSchema,indexes[j]);
                    ClusStatManager newStatManager = new ClusStatManager(newSchema,node.getSchema().getSettings());
                    newStatManager.initStatisticAndStatManager();

                    childHorizontal.initClusteringStat(newStatManager, newData);
                    childHorizontal.initTargetStat(newStatManager, newData);

                    initializeAttributeWeights(newStatManager);

                    node.getStatManager().initClusteringWeights(getNameAttributes(node.getSchema()));

                    ClusNodePBCT newPBCTNode = new ClusNodePBCT(childHorizontal, childVertical, newStatManager, node.getVerticalStatManager(), newSchema, node.getVerticalSchema(), newData, subsets[j]);

                    node.setChild(newPBCTNode,j);
                    double factor = (double)(subsets[j].getNbRows())/(double)(subsets[0].getNbRows()+subsets[1].getNbRows());
                    newPBCTNode.m_FTest = (node.m_FTest*factor);
                    
                    heur[j] = induceHorizontalOneLevel(newPBCTNode);
                    nodes[j] = newPBCTNode;
            }
            
            double count = 0.0;
            double sum = 0.0;
            for(int j = 0; j<arity; j++){
                if(heur[j]!=Double.NEGATIVE_INFINITY){
                    sum+=heur[j]*((double)subsets[j].getNbRows());
                    count++;
                }
            }
            
            if(count==0.0) return Double.NEGATIVE_INFINITY;
            //else if((count > 1.0) && ((nodes[0].getFindBestTest().getBestTest().m_SplitAttr.getName()==nodes[1].getFindBestTest().getBestTest().m_SplitAttr.getName())&&(nodes[0].getFindBestTest().getBestTest().m_BestSplit==nodes[1].getFindBestTest().getBestTest().m_BestSplit))) return Double.NEGATIVE_INFINITY;
            return (sum/((double)subsets[0].getNbRows()+(double)subsets[1].getNbRows()));
        }
        
        
        
        public final double induceHorizontalOneLevel(ClusNodePBCT node) throws ClusException{
            initSelectorAndSplit(node, node.getNodeHorizontal().getClusteringStat(), HORIZONTAL_DATA);
            
            getSettings().setFTest(node.m_FTest);
            
            if (initSelectorAndStopCrit(node, HORIZONTAL_DATA)) {
		return Double.NEGATIVE_INFINITY;
            }
            
            ClusAttrType[] attrs = getDescriptiveAttributes(node,HORIZONTAL_DATA);
            for (int i = 0; i < attrs.length; i++) {
                    ClusAttrType at = attrs[i];
                    if (at instanceof NominalAttrType) node.getFindBestTest().findNominal((NominalAttrType)at, node.getData());
                    else node.getFindBestTest().findNumeric((NumericAttrType)at, node.getData());
            }
            
            if(node.getFindBestTest().getBestTest().hasBestTest()){
                return node.getFindBestTest().getBestTest().getHeuristicValue();
            }
            else{
                return Double.NEGATIVE_INFINITY;
            }
        }
}
