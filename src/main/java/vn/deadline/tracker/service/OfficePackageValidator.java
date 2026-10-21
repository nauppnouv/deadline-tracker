package vn.deadline.tracker.service;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import vn.deadline.tracker.domain.RuleViolation;

/** Bounded OPC/OOXML format checks, not an antivirus or a complete Office schema validator. */
final class OfficePackageValidator {
    private static final long EXPANDED_LIMIT=40L*1024*1024;
    private static final int ENTRY_LIMIT=500;
    private static final String CT="http://schemas.openxmlformats.org/package/2006/content-types";
    private static final String REL="http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String OFFICE="http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    private static final String STRICT_OFFICE="http://purl.oclc.org/ooxml/officeDocument/relationships/";
    private static final Set<String> WORD_NS=Set.of("http://schemas.openxmlformats.org/wordprocessingml/2006/main","http://purl.oclc.org/ooxml/wordprocessingml/main");
    private static final Set<String> SHEET_NS=Set.of("http://schemas.openxmlformats.org/spreadsheetml/2006/main","http://purl.oclc.org/ooxml/spreadsheetml/main");
    private static final String DOC_TYPE="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    private static final String XLS_TYPE="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    private static final String SHEET_TYPE="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml";

    static void validate(Path file,String extension)throws IOException {
        try(ZipFile central=new ZipFile(file.toFile());var input=new ZipInputStream(Files.newInputStream(file))) {
            var directory=new HashMap<String,ZipEntry>();var entries=central.entries();
            while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();String name=e.getName();safeName(name);
                if(directory.size()>=ENTRY_LIMIT||directory.putIfAbsent(name,e)!=null)bad();
                if(name.toLowerCase(Locale.ROOT).contains("vbaproject"))bad();
            }
            var seen=new HashSet<String>();var xml=new HashMap<String,XmlPart>();long[] expanded={0};
            for(ZipEntry local;(local=input.getNextEntry())!=null;){
                String name=local.getName();var expected=directory.get(name);
                if(expected==null||!seen.add(name)||expected.isDirectory()!=local.isDirectory()||expected.getMethod()!=local.getMethod())bad();
                var bounded=new EntryInput(input,expanded);
                String lower=name.toLowerCase(Locale.ROOT);
                if(!local.isDirectory()&&(lower.endsWith(".xml")||lower.endsWith(".rels"))){
                    var part=new XmlPart(name);parse(bounded,part);xml.put(name,part);
                }
                byte[] buffer=new byte[8192];while(bounded.read(buffer)!=-1){/* Drain every entry, including non-XML. */}
                input.closeEntry(); // ZipInputStream also verifies local CRC/data descriptor.
                if(bounded.count!=expected.getSize()||bounded.crc.getValue()!=expected.getCrc()
                    ||bounded.count!=local.getSize()||local.getCrc()!=expected.getCrc()
                    ||local.getCompressedSize()!=expected.getCompressedSize()
                    ||bounded.count>100L*Math.max(1,expected.getCompressedSize()))bad();
            }
            if(!seen.equals(directory.keySet()))bad();
            verifyPackage(extension,directory,xml);
        }catch(ZipException|EOFException e){bad();}
    }

    private static final class EntryInput extends FilterInputStream {
        final long[] total;final CRC32 crc=new CRC32();long count;
        EntryInput(InputStream in,long[] total){super(in);this.total=total;}
        private void add(byte[] b,int off,int n){if(n<=0)return;count+=n;total[0]+=n;if(total[0]>EXPANDED_LIMIT)bad();crc.update(b,off,n);}
        @Override public int read()throws IOException{int b=in.read();if(b!=-1)add(new byte[]{(byte)b},0,1);return b;}
        @Override public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);add(b,off,n);return n;}
        @Override public void close(){/* XML parser must not close the enclosing archive. */}
    }
    private static void parse(InputStream input,XmlPart part)throws IOException {
        try{
            var factory=SAXParserFactory.newInstance();factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
            factory.setXIncludeAware(false);
            var reader=factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            reader.setEntityResolver((publicId,systemId)->{throw new SAXException("External entities forbidden");});
            reader.setContentHandler(part);reader.setErrorHandler(new DefaultHandler(){
                @Override public void error(SAXParseException e)throws SAXException{throw e;}
                @Override public void fatalError(SAXParseException e)throws SAXException{throw e;}
            });
            reader.parse(new InputSource(input));
        }catch(ParserConfigurationException|SAXException e){bad();}
    }
    private record Relationship(String type,String target,boolean external){}
    private static final class XmlPart extends DefaultHandler {
        final String name;String root,namespace,secondElement;int depth;boolean body,sheetData;
        final Map<String,String> overrides=new HashMap<>(),defaults=new HashMap<>();
        final Map<String,Relationship> relationships=new HashMap<>();final Set<String> sheetIds=new HashSet<>();
        XmlPart(String name){this.name=name;}
        @Override public void startElement(String uri,String local,String qName,Attributes attrs){
            if(++depth>256)bad();
            if(depth==1){root=local;namespace=uri;}
            if(depth==2)secondElement=local;
            if(name.equals("[Content_Types].xml")){
                if(depth==1&&(!local.equals("Types")||!CT.equals(uri)))bad();
                if(depth==2){
                    if(!CT.equals(uri))bad();String type=required(attrs,"ContentType");
                    String lower=type.toLowerCase(Locale.ROOT);if(lower.contains("macroenabled")||lower.contains("vbaproject"))bad();
                    if(local.equals("Override")){String key=required(attrs,"PartName");if(!key.startsWith("/"))bad();safeName(key.substring(1));if(overrides.putIfAbsent(key.substring(1),type)!=null)bad();}
                    else if(local.equals("Default")){String key=required(attrs,"Extension").toLowerCase(Locale.ROOT);if(!key.matches("[a-z0-9]+")||defaults.putIfAbsent(key,type)!=null)bad();}
                    else bad();
                    if(overrides.size()+defaults.size()>ENTRY_LIMIT)bad();
                }else if(depth>2)bad();
            }else if(name.toLowerCase(Locale.ROOT).endsWith(".rels")){
                if(depth==1&&(!local.equals("Relationships")||!REL.equals(uri)))bad();
                if(depth==2){
                    if(!REL.equals(uri)||!local.equals("Relationship"))bad();
                    String mode=attrs.getValue("TargetMode");if(mode!=null&&!mode.equals("Internal")&&!mode.equals("External"))bad();
                    var rel=new Relationship(required(attrs,"Type"),required(attrs,"Target"),"External".equals(mode));
                    if(relationships.putIfAbsent(required(attrs,"Id"),rel)!=null||relationships.size()>ENTRY_LIMIT)bad();
                }else if(depth>2)bad();
            }
            if(depth==2&&local.equals("body")&&WORD_NS.contains(uri)&&uri.equals(namespace))body=true;
            if(depth==2&&local.equals("sheetData")&&SHEET_NS.contains(uri)&&uri.equals(namespace))sheetData=true;
            if(depth==3&&"sheets".equals(secondElement)&&local.equals("sheet")&&SHEET_NS.contains(uri)&&uri.equals(namespace)){
                String id=attrs.getValue(OFFICE.substring(0,OFFICE.length()-1),"id");
                if(id==null)id=attrs.getValue(STRICT_OFFICE.substring(0,STRICT_OFFICE.length()-1),"id");
                if(id==null||id.isBlank()||!sheetIds.add(id)||sheetIds.size()>ENTRY_LIMIT)bad();
            }
        }
        @Override public void endElement(String uri,String local,String qName){depth--;}
        String type(String part){String result=overrides.get(part);if(result!=null)return result;int dot=part.lastIndexOf('.');return defaults.get(dot<0?"":part.substring(dot+1).toLowerCase(Locale.ROOT));}
    }
    private static String required(Attributes attrs,String key){String value=attrs.getValue(key);if(value==null||value.isBlank()||value.length()>2048)bad();return value;}
    private static void verifyPackage(String extension,Map<String,ZipEntry> entries,Map<String,XmlPart> xml){
        var types=xml.get("[Content_Types].xml");var rels=xml.get("_rels/.rels");if(types==null||rels==null)bad();
        for(var e:entries.values())if(!e.isDirectory()&&!e.getName().equals("[Content_Types].xml")){
            String type=types.type(e.getName());if(type==null)bad();
            if((type.endsWith("+xml")||type.equals("application/xml"))&&!xml.containsKey(e.getName()))bad();
            if(e.getName().toLowerCase(Locale.ROOT).endsWith(".rels")&&!type.equals("application/vnd.openxmlformats-package.relationships+xml"))bad();
        }
        for(String name:types.overrides.keySet())if(!entries.containsKey(name)||entries.get(name).isDirectory())bad();
        var mains=rels.relationships.values().stream().filter(r->relationshipType(r,"officeDocument")).toList();if(mains.size()!=1)bad();
        var main=mains.getFirst();String target=internalTarget("",main);
        boolean doc=extension.equals("docx");String required=doc?"word/document.xml":"xl/workbook.xml";
        if(!target.equals(required)||!Objects.equals(types.type(target),doc?DOC_TYPE:XLS_TYPE))bad();
        var content=xml.get(target);if(content==null)bad();
        if(doc){if(!content.root.equals("document")||!WORD_NS.contains(content.namespace)||!content.body)bad();}
        else{
            if(!content.root.equals("workbook")||!SHEET_NS.contains(content.namespace)||content.sheetIds.isEmpty())bad();
            var workbookRels=xml.get("xl/_rels/workbook.xml.rels");if(workbookRels==null)bad();
            for(String id:content.sheetIds){
                var rel=workbookRels.relationships.get(id);if(rel==null||!relationshipType(rel,"worksheet"))bad();
                String sheet=internalTarget("xl/",rel);var part=xml.get(sheet);
                if(!SHEET_TYPE.equals(types.type(sheet))||part==null||!part.root.equals("worksheet")||!SHEET_NS.contains(part.namespace)||!part.sheetData)bad();
            }
        }
    }
    private static boolean relationshipType(Relationship rel,String suffix){return rel.type.equals(OFFICE+suffix)||rel.type.equals(STRICT_OFFICE+suffix);}
    private static String internalTarget(String base,Relationship rel){
        if(rel.external)bad();
        try{var uri=URI.create(rel.target);if(uri.isAbsolute()||uri.getRawAuthority()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)bad();
            String name=uri.getPath();if(name==null)bad();String resolved=name.startsWith("/")?name.substring(1):base+name;safeName(resolved);return resolved;
        }catch(IllegalArgumentException e){bad();return null;}
    }
    private static void safeName(String name){
        if(name.isEmpty()||name.length()>1024||name.startsWith("/")||name.contains("\\")||name.contains(":")||name.contains("%")||name.chars().anyMatch(c->c<32))bad();
        for(String segment:name.split("/",-1))if(segment.equals("..")||segment.equals("."))bad();
    }
    private static void bad(){throw RuleViolation.invalid("Nội dung tệp không khớp định dạng được phép.");}
}
