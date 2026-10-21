package vn.deadline.tracker;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Synthetic, minimal OPC packages; never user documents. */
final class OoxmlFixtures {
    static final String CT="http://schemas.openxmlformats.org/package/2006/content-types";
    static final String REL="http://schemas.openxmlformats.org/package/2006/relationships";
    static final String OFFICE="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String WORD="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String SHEET="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    static final String DOC_TYPE="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    static final String XLS_TYPE="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    static final String WORKSHEET_TYPE="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml";
    static LinkedHashMap<String,byte[]> parts(String ext){
        var p=new LinkedHashMap<String,byte[]>();boolean doc=ext.equals("docx");String main=doc?"word/document.xml":"xl/workbook.xml";
        put(p,"[Content_Types].xml","<Types xmlns=\""+CT+"\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/"+main+"\" ContentType=\""+(doc?DOC_TYPE:XLS_TYPE)+"\"/>"+(doc?"":"<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\""+WORKSHEET_TYPE+"\"/>")+"</Types>");
        put(p,"_rels/.rels","<Relationships xmlns=\""+REL+"\"><Relationship Id=\"rId1\" Type=\""+OFFICE+"/officeDocument\" Target=\""+main+"\"/></Relationships>");
        if(doc)put(p,main,"<w:document xmlns:w=\""+WORD+"\"><w:body><w:p><w:r><w:t>Synthetic document</w:t></w:r></w:p></w:body></w:document>");
        else{
            put(p,main,"<workbook xmlns=\""+SHEET+"\" xmlns:r=\""+OFFICE+"\"><sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(p,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\""+REL+"\"><Relationship Id=\"rId1\" Type=\""+OFFICE+"/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(p,"xl/worksheets/sheet1.xml","<worksheet xmlns=\""+SHEET+"\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Synthetic sheet</t></is></c></row></sheetData></worksheet>");
        }return p;
    }
    static void put(Map<String,byte[]> parts,String name,String text){parts.put(name,text.getBytes(StandardCharsets.UTF_8));}
    static byte[] zip(Map<String,byte[]> parts)throws IOException{return zip(parts,false);}
    static byte[] zip(Map<String,byte[]> parts,boolean stored)throws IOException{
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){
            for(var part:parts.entrySet()){var e=new ZipEntry(part.getKey());if(stored){e.setMethod(ZipEntry.STORED);e.setSize(part.getValue().length);var crc=new CRC32();crc.update(part.getValue());e.setCrc(crc.getValue());}zip.putNextEntry(e);zip.write(part.getValue());zip.closeEntry();}
        }return out.toByteArray();
    }
    static byte[] changeCentral(byte[] input,String name,int offset,int value){
        byte[] b=input.clone();var view=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<=b.length-46;i++)if(view.getInt(i)==0x02014b50){int n=Short.toUnsignedInt(view.getShort(i+28));if(i+46+n<=b.length&&new String(b,i+46,n,StandardCharsets.UTF_8).equals(name)){view.putInt(i+offset,value);return b;}}
        throw new IllegalArgumentException(name);
    }
}
