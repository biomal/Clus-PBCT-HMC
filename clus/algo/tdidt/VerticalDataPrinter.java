/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clus.algo.tdidt;

import clus.algo.tdidt.PBCTApproach;
import clus.data.type.ClusAttrType;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

// ********************************
// PBCT-HMC
// author: @zamith
// ********************************
public class VerticalDataPrinter {
    PrintWriter m_Printer;
    PBCTApproach m_Approach;
    
    public VerticalDataPrinter(PBCTApproach approach) throws FileNotFoundException{
        this.m_Approach = approach;
        this.m_Printer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("outputVerticalData.arff")));
        this.exportDataVertical();
        this.m_Printer.close();
    }
    
    public void exportDataVertical() throws FileNotFoundException{
        writeRelation();
        writeDescriptiveAttributes();
        writeTargetAttributes();
        writeData();
    }
    
    private void writeRelation(){
        m_Printer.println("@RELATION VerticalData");
        m_Printer.println();
    }
    
    private void writeDescriptiveAttributes(){
        for(int i=0; i<m_Approach.getDescriptiveAttrsVertical().length; i++){
            m_Printer.println("@ATTRIBUTE "+m_Approach.getDescriptiveAttrsVertical()[i].getName()+" numeric");
        }
    }
    
    private void writeTargetAttributes(){
        for(int i=0; i<m_Approach.getTargetAttrsVertical().length; i++){
            m_Printer.println("@ATTRIBUTE "+m_Approach.getTargetAttrsVertical()[i].getName()+" numeric");
        }
    }
    
    private void writeData(){
        m_Printer.println();
        m_Printer.println("@DATA");
        for(int i=0; i<m_Approach.getDataRowsVertical().length; i++){
            m_Printer.println(m_Approach.getDataRowsVertical()[i]);
        }
    }
}
