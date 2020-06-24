/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clus.algo.tdidt;

import clus.algo.tdidt.VerticalDataPrinter;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.data.type.NumericAttrType;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

// ********************************
// PBCT-HMC
// author: @zamith
// ********************************
public abstract class PBCTApproach {
    ClusSchema m_SchemaHorizontal;
    ClusSchema m_SchemaVertical;
    
    String m_HierSeparator;
    
    ClusAttrType[] m_TargetAttrsHorizontal;
    RowData m_DataHorizontal;
    
    ClusAttrType[] m_DescriptiveAttrsVertical;
    ClusAttrType[] m_TargetAttrsVertical;
    String[] m_DataRowsVertical;
    
    public ClusAttrType[] getDescriptiveAttrsVertical(){
        return this.m_DescriptiveAttrsVertical;
    }
    
    public ClusAttrType[] getTargetAttrsVertical(){
        return this.m_TargetAttrsVertical;
    }
    
    public String[] getDataRowsVertical(){
        return this.m_DataRowsVertical;
    }
    
    public RowData getDataHorizontal() {
        return m_DataHorizontal;
    }

    public void setDataHorizontal(RowData dataHorizontal) {
        this.m_DataHorizontal = dataHorizontal;
    }

    public ClusSchema getSchemaHorizontal() {
        return m_SchemaHorizontal;
    }

    public void setSchemaHorizontal(ClusSchema schemaHorizontal) {
        this.m_SchemaHorizontal = schemaHorizontal;
        this.m_TargetAttrsHorizontal = schemaHorizontal.getTargetAttributes();
    }

    public String getHierSeparator() {
        return m_HierSeparator;
    }

    public void setHierSeparator(String hierSeparator) {
        this.m_HierSeparator = hierSeparator;
    }
    
    private void setDescriptiveAttrs(){ }
    
    private void setTargetAttrs(){ }
    
    private void setDataVertical(){ }
    
    private void exportDataVertical() throws FileNotFoundException{ }
    
    private String getDescriptives(ClusAttrType row){ return null; }
    
    private String getTargets(ClusAttrType row){ return null; }
    
    private boolean isSubtree(ClusAttrType rowAttr, ClusAttrType columnAttr){ return false; }
    
    private boolean isContained(ClusAttrType rowAttr, int column){ return false; }
}
