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

package clus.pruning;

import java.util.ArrayList;

import clus.algo.tdidt.*;
import clus.data.rows.RowData;
import clus.util.*;
import clus.heuristic.*;
import clus.model.test.NodeTest;


public class EncodingCostPruning extends PruneTree {
	
	public double m_Ecc; // encoding cost
	public double m_EccGain = 0;
	public ClusNode m_BestNodeToPrune;
	public RowData m_Data; // RowData at root node of original tree
	public EncodingCost m_EC;
	
	public EncodingCostPruning() {
		m_EC = new EncodingCost();
	}

	public void setTrainingData(RowData data) {
		m_Data = data;
		m_EC.setAttributes(m_Data.getSchema().getDescriptiveAttributes());
	}
	
	public int getNbResults() {
		return 1;
	}
	
	public void prune(ClusNode node) throws ClusException {
		node.numberCompleteTree();
		int totalNbNodes = node.getTotalTreeSize();
		m_EC.initializeLogPMatrix(totalNbNodes);
		doPrune(node);
		//m_EC.printDuration();
	}
	
	public void doPrune(ClusNode node) throws ClusException {
		System.out.println("Pruning!");
		m_Ecc = calculateEncodingCost(node,m_Data);
		System.out.println(" -> orig ecc = " + m_Ecc);
		traverseTreeAndRecordEncodingCostIfLeafChildren(node, node, m_Data);
		if ((m_EccGain > 0) && (m_BestNodeToPrune != null)) {
			System.out.println("Pruning node such that ECC drops with " + m_EccGain);
			m_BestNodeToPrune.makeLeaf();
			m_EccGain = 0;
			m_BestNodeToPrune = null;
			doPrune(node);
		}
	}
	
	public double calculateEncodingCost(ClusNode node, RowData data) {
		ArrayList<RowData> clusters = new ArrayList<RowData>();
		ArrayList<Integer> clusterIds = new ArrayList<Integer>();
		getLeafClusters(node, data, clusters, clusterIds);
		m_EC.setClusters(clusters, clusterIds);
		double ecv = m_EC.getEncodingCostValue();
		return ecv;
	}
	
	/*
	 * Traverse tree in post-order. Each time a node is visited that has 2 leaf children, calculate the encoding
	 * cost for merging the leaf children. Record the node that gives the highest encoding cost reduction.
	 */
	public int traverseTreeAndRecordEncodingCostIfLeafChildren(ClusNode node, ClusNode rootNode, RowData rootData) {
		int arity = node.getNbChildren();
		if (arity > 0) {
			int nbLeafChildren = 0;
			for (int i = 0; i < arity; i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				nbLeafChildren += traverseTreeAndRecordEncodingCostIfLeafChildren(child, rootNode, rootData);
			}
			if (nbLeafChildren == arity) {
				// make leaf node
				ClusNode[] children = node.getChildren();
				NodeTest test = node.getTest();
				node.makeLeaf();
				// calculate ecc
				double ecc = calculateEncodingCost(rootNode,rootData);
				System.out.println("new ecc = " + ecc);
				double eccGain = m_Ecc - ecc;
				if (eccGain > m_EccGain) {
					m_EccGain = eccGain;
					m_BestNodeToPrune = node;
					System.out.println("better!");
				}
				// reset node
				node.setTest(test);
				for (int i = 0; i < children.length; i++) {
					node.addChild(children[i]);
				}
			}
			return 0;
		}
		else {
			return 1;
		}

	}
	
	
	public void traverseTreeAndRecordEncodingCost(ClusNode node, ClusNode rootNode, RowData rootData) {
		int arity = node.getNbChildren();
		if (arity > 0) {
			for (int i = 0; i < arity; i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				traverseTreeAndRecordEncodingCost(child, rootNode, rootData);
			}
			// make leaf node
			ClusNode[] children = node.getChildren();
			NodeTest test = node.getTest();
			node.makeLeaf();
			// calculate ecc
			double ecc = calculateEncodingCost(rootNode,rootData);
			System.out.println("new ecc = " + ecc);
			double eccGain = m_Ecc - ecc;
			if (eccGain > m_EccGain) {
				m_EccGain = eccGain;
				m_BestNodeToPrune = node;
				System.out.println("better!");
			}
			// reset node
			node.setTest(test);
			for (int i = 0; i < children.length; i++) {
				node.addChild(children[i]);
			}
		}

	}
	
	
	public void getLeafClusters(ClusNode node, RowData data, ArrayList<RowData> clusters, ArrayList<Integer> clusterIds) {
		if (!node.atBottomLevel()) {
			int arity = node.getNbChildren();
			for (int i = 0; i < arity; i++) {
				RowData subset = data.applyWeighted(node.getTest(), i);
				getLeafClusters((ClusNode)node.getChild(i), subset, clusters, clusterIds);
			}
		}
		else {
			clusters.add(data);
			clusterIds.add(new Integer(node.getID()-1));
			//System.out.println("cluster = " + (node.getID()-1));
		}
	}
	
}