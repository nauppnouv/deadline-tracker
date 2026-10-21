package vn.deadline.tracker;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
import static vn.deadline.tracker.OoxmlFixtures.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="server.address=127.0.0.1")
class OoxmlUploadTest extends W11HttpTestSupport {
    User actor;String group,task;HttpClient client;
    @BeforeAll void loginOnce()throws Exception{actor=user();client=login(actor);}
    @AfterAll void closeClient(){if(client!=null)client.close();}
    @BeforeEach void seed(){group=work.createGroup(actor.id(),"OOXML format regression","").getId();task=work.createTask(actor.id(),group,"OOXML upload","",actor.id(),Instant.now().plusSeconds(86400)).getId();}
    String path(){return "/groups/"+group+"/tasks/"+task+"/attachments";}
    int upload(HttpClient c,String ext,byte[] bytes)throws Exception{
        String boundary="w11"+UUID.randomUUID().toString();var out=new ByteArrayOutputStream();
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"synthetic."+ext+"\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        return c.send(request(path()).header("X-CSRF-TOKEN",token(c,"/account")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    Set<String> stored()throws IOException {try(var paths=Files.list(Path.of(attachmentRoot))){return paths.map(p->p.getFileName().toString()).collect(java.util.stream.Collectors.toSet());}}
    void reject(String ext,byte[] data)throws Exception{
        var before=stored();{var c=client;
            int status=upload(c,ext,data);long rows=jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Long.class,task);
            assertAll(()->assertEquals(400,status,"Malformed OOXML must be rejected before READY"),()->assertEquals(0,rows),()->assertEquals(before,stored(),"No leaked staged or blob files"),()->assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM task_pin WHERE task_id=?",Integer.class,task)));
        }
    }
    @ParameterizedTest @ValueSource(strings={"docx","xlsx"})
    void validPackageReachesReadyAndDownloadsIdenticalBytes(String ext)throws Exception{
        byte[] data=zip(parts(ext));{var c=client;
            assertEquals(302,upload(c,ext,data));var row=jdbc.queryForMap("SELECT * FROM task_attachment WHERE task_id=?",task);
            assertEquals("READY",row.get("lifecycle"));assertEquals((long)data.length,((Number)row.get("size_bytes")).longValue());assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)),row.get("sha256"));
            assertArrayEquals(data,Files.readAllBytes(Path.of(attachmentRoot).resolve(row.get("storage_key")+".blob")));
            var download=c.send(request(path()+"/"+row.get("id")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,download.statusCode());assertArrayEquals(data,download.body());
            assertTrue(download.headers().firstValue("Content-Type").orElseThrow().contains(ext.equals("docx")?"wordprocessingml.document":"spreadsheetml.sheet"));
        }
    }
    static Stream<Arguments> malformed()throws Exception {
        var cases=new ArrayList<Arguments>();
        for(String ext:List.of("docx","xlsx")){
            String main=ext.equals("docx")?"word/document.xml":"xl/workbook.xml";
            for(String defect:List.of("plain","missing-types","broken-types","wrong-type","wrong-types-namespace","missing-main","broken-main","wrong-main-root","missing-relationship","wrong-target","external-main","macro-name","macro-type","duplicate-type","doctype")){
                var p=parts(ext);
                switch(defect){
                    case "plain"->{put(p,"[Content_Types].xml","not XML; synthetic invalid document");put(p,main,"not XML; synthetic invalid document");}
                    case "missing-types"->p.remove("[Content_Types].xml");
                    case "broken-types"->put(p,"[Content_Types].xml","<Types>");
                    case "wrong-type"->put(p,"[Content_Types].xml",new String(p.get("[Content_Types].xml"),StandardCharsets.UTF_8).replace(ext.equals("docx")?DOC_TYPE:XLS_TYPE,"text/plain"));
                    case "wrong-types-namespace"->put(p,"[Content_Types].xml",new String(p.get("[Content_Types].xml"),StandardCharsets.UTF_8).replace(CT,"urn:wrong"));
                    case "missing-main"->p.remove(main);
                    case "broken-main"->put(p,main,"<broken>");
                    case "wrong-main-root"->put(p,main,"<document xmlns=\"urn:wrong\"/>");
                    case "missing-relationship"->p.remove("_rels/.rels");
                    case "wrong-target"->put(p,"_rels/.rels",new String(p.get("_rels/.rels"),StandardCharsets.UTF_8).replace(main,"missing.xml"));
                    case "external-main"->put(p,"_rels/.rels",new String(p.get("_rels/.rels"),StandardCharsets.UTF_8).replace("Target=","TargetMode=\"External\" Target="));
                    case "macro-name"->p.put("word/vbaProject.bin",new byte[]{1,2,3});
                    case "macro-type"->put(p,"[Content_Types].xml",new String(p.get("[Content_Types].xml"),StandardCharsets.UTF_8).replace(ext.equals("docx")?DOC_TYPE:XLS_TYPE,"application/vnd.ms-word.document.macroEnabled.main+xml"));
                    case "duplicate-type"->put(p,"[Content_Types].xml",new String(p.get("[Content_Types].xml"),StandardCharsets.UTF_8).replace("</Types>","<Override PartName=\"/"+main+"\" ContentType=\"text/plain\"/></Types>"));
                    default->put(p,main,"<!DOCTYPE x [<!ENTITY internal 'synthetic'>]><x>&internal;</x>");
                }cases.add(Arguments.of(ext+"-"+defect,ext,zip(p)));
            }
        }
        var p=parts("docx");put(p,"word/document.xml","<w:document xmlns:w=\""+WORD+"\"/>");cases.add(Arguments.of("missing-word-body","docx",zip(p)));
        p=parts("docx");put(p,"word/document.xml","<w:document xmlns:w=\""+WORD+"\"><w:body>"+"<x>".repeat(257)+"</x>".repeat(257)+"</w:body></w:document>");cases.add(Arguments.of("xml-depth-limit","docx",zip(p)));
        p=parts("xlsx");put(p,"xl/workbook.xml",new String(p.get("xl/workbook.xml"),StandardCharsets.UTF_8).replace("sheets>","wrong>"));cases.add(Arguments.of("wrong-sheets-parent","xlsx",zip(p)));
        for(String defect:List.of("missing-sheet","broken-sheet","wrong-sheet-type","missing-sheet-rel")){
            p=parts("xlsx");switch(defect){case "missing-sheet"->p.remove("xl/worksheets/sheet1.xml");case "broken-sheet"->put(p,"xl/worksheets/sheet1.xml","<worksheet>");case "wrong-sheet-type"->put(p,"[Content_Types].xml",new String(p.get("[Content_Types].xml"),StandardCharsets.UTF_8).replace(WORKSHEET_TYPE,"text/plain"));default->p.remove("xl/_rels/workbook.xml.rels");}cases.add(Arguments.of(defect,"xlsx",zip(p)));
        }
        return cases.stream();
    }
    @ParameterizedTest(name="{0}") @MethodSource("malformed")
    void malformedPackagesLeaveDatabaseAndStorageUnchanged(String label,String ext,byte[] data)throws Exception{reject(ext,data);}

    @Test void dtdAndExternalEntitiesCannotContactLoopbackServer()throws Exception {
        var requests=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{requests.incrementAndGet();byte[] b="synthetic entity".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,b.length);try(var out=exchange.getResponseBody()){out.write(b);}});server.start();
        try{
            String url="http://127.0.0.1:"+server.getAddress().getPort()+"/entity";
            for(String declaration:List.of("<!ENTITY xxe SYSTEM '"+url+"'>","<!ENTITY % remote SYSTEM '"+url+"'>%remote;")){
                var p=parts("docx");put(p,"word/document.xml","<!DOCTYPE w:document ["+declaration+"]><w:document xmlns:w=\""+WORD+"\"><w:body><w:p>&xxe;</w:p></w:body></w:document>");reject("docx",zip(p));
            }assertEquals(0,requests.get(),"No external resolution/socket request is permitted");
        }finally{server.stop(0);}
    }
    @Test void fileEntityAndExternalDtdAreRejected()throws Exception {
        Path sentinel=Files.createTempFile("w11-xxe-sentinel-",".txt");Files.writeString(sentinel,"Synthetic private sentinel");
        try{for(String dtd:List.of("<!DOCTYPE w:document [<!ENTITY xxe SYSTEM '"+sentinel.toUri()+"'>]>","<!DOCTYPE w:document SYSTEM '"+sentinel.toUri()+"'>")){
            var p=parts("docx");put(p,"word/document.xml",dtd+"<w:document xmlns:w=\""+WORD+"\"><w:body><w:p>&xxe;</w:p></w:body></w:document>");reject("docx",zip(p));
        }}finally{Files.delete(sentinel);}
    }
    @Test void crcMismatchInNonXmlEntryIsRejected()throws Exception {
        var p=parts("docx");p.put("word/media/image.bin",new byte[]{1,2,3,4});reject("docx",changeCentral(zip(p,true),"word/media/image.bin",16,123456));
    }
    @Test void truncatedArchiveIsRejected()throws Exception {byte[] b=zip(parts("docx"));reject("docx",Arrays.copyOf(b,b.length-30));}
    @Test void entryCountAndCompressionRatioAreBounded()throws Exception {
        var p=parts("docx");for(int i=0;i<501;i++)p.put("extras/"+i+".bin",new byte[]{1});reject("docx",zip(p));
        p=parts("docx");p.put("word/media/bomb.bin",new byte[1024*1024]);reject("docx",zip(p));
    }
    @Test void forgedDeclaredSizesCannotBypassActualExpandedByteLimit()throws Exception {
        var p=parts("docx");byte[] data=new byte[4*1024*1024];byte[] block=new byte[512*1024];new Random(1103).nextBytes(block);System.arraycopy(block,0,data,0,block.length);
        for(int i=0;i<11;i++)p.put("extras/"+i+".bin",data);
        byte[] b=zip(p);assertTrue(b.length<10*1024*1024);
        reject("docx",b); // Actual total exceeds 40 MiB, each entry's compression ratio stays below 100.
        for(int i=0;i<11;i++)b=changeCentral(b,"extras/"+i+".bin",24,0);
        reject("docx",b);
    }
    @Test void traversalAndDuplicateNamesAreRejected()throws Exception {
        for(String name:List.of("../evil.bin","/absolute.bin","word\\evil.bin")){var p=parts("docx");p.put(name,new byte[]{1});reject("docx",zip(p));}
        var p=parts("docx");put(p,"extras/a.xml","<x/>");put(p,"extras/b.xml","<x/>");byte[] b=zip(p);
        byte[] from="extras/b.xml".getBytes(StandardCharsets.UTF_8),to="extras/a.xml".getBytes(StandardCharsets.UTF_8);
        for(int i=0;i<=b.length-from.length;i++){if(Arrays.equals(Arrays.copyOfRange(b,i,i+from.length),from))System.arraycopy(to,0,b,i,to.length);}
        reject("docx",b);
    }
}
