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
public class SubtreeApproach extends PBCTApproach{
 
    ClusSchema m_SchemaHorizontal;
    ClusSchema m_SchemaVertical;
    
    String m_HierSeparator;
    
    ClusAttrType[] m_TargetAttrsHorizontal;
    RowData m_DataHorizontal;
    
    ClusAttrType[] m_DescriptiveAttrsVertical;
    ClusAttrType[] m_TargetAttrsVertical;
    String[] m_DataRowsVertical;
    
    public SubtreeApproach(RowData dataHorizontal, ClusSchema schemaHorizontal, String hierSeparator) throws FileNotFoundException{
        this.setDataHorizontal(dataHorizontal);
        this.setSchemaHorizontal(schemaHorizontal);
        this.setHierSeparator(hierSeparator);
        this.setDescriptiveAttrs();
        this.setTargetAttrs();
        this.setDataVertical();
        this.exportDataVertical();
    }
    
    @Override
    public ClusAttrType[] getDescriptiveAttrsVertical(){
        return this.m_DescriptiveAttrsVertical;
    }
    
    @Override
    public ClusAttrType[] getTargetAttrsVertical(){
        return this.m_TargetAttrsVertical;
    }
    
    @Override
    public String[] getDataRowsVertical(){
        return this.m_DataRowsVertical;
    }

    @Override
    public RowData getDataHorizontal() {
        return m_DataHorizontal;
    }

    @Override
    public void setDataHorizontal(RowData dataHorizontal) {
        this.m_DataHorizontal = dataHorizontal;
    }

    @Override
    public ClusSchema getSchemaHorizontal() {
        return m_SchemaHorizontal;
    }

    @Override
    public void setSchemaHorizontal(ClusSchema schemaHorizontal) {
        this.m_SchemaHorizontal = schemaHorizontal;
        this.m_TargetAttrsHorizontal = schemaHorizontal.getTargetAttributes();
    }

    @Override
    public String getHierSeparator() {
        return m_HierSeparator;
    }

    @Override
    public void setHierSeparator(String hierSeparator) {
        this.m_HierSeparator = hierSeparator;
    }
    
    private void setDescriptiveAttrs(){       
        Set<NumericAttrType> attrs = new HashSet<NumericAttrType>();
        for(int i=0; i<m_TargetAttrsHorizontal.length; i++){
            String currAttr = m_TargetAttrsHorizontal[i].getName();
            int indexHierSeparator = currAttr.indexOf(m_HierSeparator);
            
            if(indexHierSeparator == -1)
                attrs.add(new NumericAttrType(currAttr));   
        }

        NumericAttrType[] descriptiveAttrs = new NumericAttrType[attrs.size()];
        this.m_DescriptiveAttrsVertical = attrs.toArray(descriptiveAttrs);
    }
    
    private void setTargetAttrs(){     
        int nbRows = m_DataHorizontal.m_Data.length;
        NumericAttrType[] attrs = new NumericAttrType[nbRows];
        for(int i=0; i<nbRows; i++){
            String currAttr = "Protein"+i;
            attrs[i] = new NumericAttrType(currAttr);  
        }        
        
        this.m_TargetAttrsVertical = attrs;
    }
    
    private void setDataVertical(){
        String[] rows = new String[m_TargetAttrsHorizontal.length];
        
        for(int i=0; i<m_TargetAttrsHorizontal.length; i++){
            rows[i] = this.getDescriptives(m_TargetAttrsHorizontal[i]);
            rows[i] += ",";
            rows[i] += this.getTargets(m_TargetAttrsHorizontal[i]);
        }
        
        this.m_DataRowsVertical = rows;
    }
    
    private void exportDataVertical() throws FileNotFoundException{
        new VerticalDataPrinter(this);
    }
    
    private String getDescriptives(ClusAttrType row){
        String descriptives="";
        
        for(int i=0; i<m_DescriptiveAttrsVertical.length;i++){
            if(i>0)
                descriptives += ",";
            
            if(isSubtree(row,m_DescriptiveAttrsVertical[i])){
                descriptives += "1.0";
            }
            else{
                descriptives += "0.0";
            }
        }
        
        return descriptives;
    }
    
    private String getTargets(ClusAttrType row){
        String targets="";
        
        for(int i=0; i<m_DataHorizontal.getNbRows();i++){
            if(i>0)
                targets += ",";
            if(isContained(row,i)){
                targets += "1.0";
            }
            else{
                targets += "0.0";
            }
        }
        
        return targets;
    }
    
    private boolean isSubtree(ClusAttrType rowAttr, ClusAttrType columnAttr){
        int indexHierSeparator = rowAttr.getName().indexOf(m_HierSeparator);
        String subRow;
        
        if(indexHierSeparator != -1){
            subRow = rowAttr.getName().substring(0,indexHierSeparator);
        } else subRow = rowAttr.getName();
                
        if(subRow.equals(columnAttr.getName())){
            return true;
        } else return false;
    }
    
    private boolean isContained(ClusAttrType rowAttr, int column){
        int nbDescriptive = m_SchemaHorizontal.getNbNumericDescriptiveAttributes();
        
        int indexContained = -1;
        for(int i=0; i<m_SchemaHorizontal.getTargetAttributes().length;i++){
            if(m_SchemaHorizontal.getTargetAttributes()[i].getName().equals(rowAttr.getName())){
                indexContained = i;
            }
        }
        
        double correspondingValue = m_DataHorizontal.m_Data[column].m_Doubles[nbDescriptive+indexContained];
              
        if(correspondingValue==1)
            return true;
        else
            return false;   
    }
    
    
}