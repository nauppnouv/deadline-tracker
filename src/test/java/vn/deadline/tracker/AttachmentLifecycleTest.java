package vn.deadline.tracker;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.service.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AttachmentLifecycleTest extends DatabaseTest {
    @Autowired AccountService accounts;@Autowired WorkService work;@Autowired AttachmentService files;
    @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;
    @Autowired CommentService comments;
    String actor,group,task;
    @BeforeEach void setup(){actor=accounts.register("a"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Attachment fixture","Synthetic attachment boundary passphrase 123!");group=work.createGroup(actor,"File lifecycle","").getId();task=work.createTask(actor,group,"File lifecycle task","",actor,Instant.now().plusSeconds(86400)).getId();}
    MockMultipartFile file(String name,String text){return new MockMultipartFile("file",name,"application/octet-stream",text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @Test void commentAttachmentIsTaskScopedAndHiddenWithItsComment()throws Exception {
        String comment=comments.create(actor,group,task,"Attachment context",List.of());
        var row=files.upload(actor,group,task,file("context.txt","synthetic context"),comment);
        assertEquals(comment,row.commentId);assertEquals(1,files.list(actor,group,task).size());
        files.pin(actor,group,task,row.id,true);
        String other=work.createTask(actor,group,"Another task","",actor,Instant.now().plusSeconds(86400)).getId();
        assertThrows(RuleViolation.class,()->files.upload(actor,group,other,file("cross.txt","cross"),comment));
        comments.hide(actor,group,task,comment);
        assertTrue(files.list(actor,group,task).isEmpty());assertTrue(files.pins(actor,group,task).isEmpty());
        assertThrows(RuleViolation.class,()->files.download(actor,group,task,row.id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE id=?",Integer.class,row.id));
    }
    @Test void interruptedInputAndTransactionRollbackNeverPublishRows()throws Exception {
        var broken=new MockMultipartFile("file","broken.txt","text/plain",new byte[]{1}){@Override public InputStream getInputStream(){return new InputStream(){int read;public int read()throws IOException{if(read++<20)return 'x';throw new IOException("Synthetic interrupted upload");}};}};
        assertThrows(IOException.class,()->files.upload(actor,group,task,broken));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Integer.class,task));
        final String[] key={null};new TransactionTemplate(manager).executeWithoutResult(tx->{try{var row=files.upload(actor,group,task,file("rollback.txt","synthetic rollback"));key[0]=row.storageKey;}catch(IOException e){throw new UncheckedIOException(e);}tx.setRollbackOnly();});
        assertNotNull(key[0]);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Integer.class,task));assertTrue(files.reconcile().orphanBlobs()>0,"Filesystem move cannot be rolled back; reconciliation must report the retained orphan");
    }
    @Test void hiddenBytesCountTowardsGroupQuotaAndConcurrentUploadsSerialize()throws Exception {
        var first=files.upload(actor,group,task,file("hidden.txt","synthetic hidden bytes"));files.hide(actor,group,task,first.id);
        jdbc.update("UPDATE task_attachment SET size_bytes=? WHERE id=?",10L*1024*1024-10,first.id);
        String fillerTask=work.createTask(actor,group,"Quota metadata fixture","",actor,Instant.now().plusSeconds(86400)).getId();
        for(int i=0;i<19;i++)jdbc.update("INSERT INTO task_attachment(id,task_id,group_id,uploader_id,display_name,media_type,size_bytes,sha256,storage_key,lifecycle,created_at) SELECT ?,?,group_id,uploader_id,display_name,media_type,?,sha256,?,'READY',created_at FROM task_attachment WHERE id=?",UUID.randomUUID().toString(),fillerTask,10L*1024*1024,UUID.randomUUID().toString(),first.id);
        try(var pool=Executors.newFixedThreadPool(2)){
            var start=new CountDownLatch(1);var futures=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++)futures.add(pool.submit(()->{start.await();try{files.upload(actor,group,task,file("race.txt","12345678"));return true;}catch(RuleViolation rejected){return false;}}));
            start.countDown();int accepted=0;for(var f:futures)if(f.get(15,TimeUnit.SECONDS))accepted++;
            assertEquals(1,accepted);assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Integer.class,task));
        }finally{jdbc.update("DELETE FROM task_attachment WHERE task_id=?",fillerTask);jdbc.update("UPDATE task_attachment SET size_bytes=? WHERE id=?","synthetic hidden bytes".getBytes().length,first.id);}
    }
    @Test void formatOfficeBombAndStreamingFileLimitAreRejected()throws Exception {
        for(String ext:List.of("pdf","png","jpg","docx","xlsx","exe"))assertThrows(RuleViolation.class,()->files.upload(actor,group,task,file("bad."+ext,"plain text")),ext);
        assertThrows(RuleViolation.class,()->files.upload(actor,group,task,new MockMultipartFile("file","nul.txt","text/plain",new byte[]{'a',0,'b'})));
        var buffer=new ByteArrayOutputStream();try(var zip=new java.util.zip.ZipOutputStream(buffer)){for(String name:List.of("[Content_Types].xml","word/document.xml","word/vbaProject.bin")){zip.putNextEntry(new java.util.zip.ZipEntry(name));zip.write("synthetic".getBytes());zip.closeEntry();}}
        assertThrows(RuleViolation.class,()->files.upload(actor,group,task,new MockMultipartFile("file","macro.docx","application/zip",buffer.toByteArray())));
        var oversized=new MockMultipartFile("file","limit.txt","text/plain",new byte[]{1}){@Override public InputStream getInputStream(){return new InputStream(){long remaining=10L*1024*1024+1;public int read(){return remaining-->0?'x':-1;}public int read(byte[] b,int off,int len){if(remaining==0)return -1;int n=(int)Math.min(remaining,len);Arrays.fill(b,off,off+n,(byte)'x');remaining-=n;return n;}};}};
        assertThrows(RuleViolation.class,()->files.upload(actor,group,task,oversized));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Integer.class,task));
    }
    @Test void matchingStagedBlobRecoversAndCorruptOrMissingBlobFailsClosed()throws Exception {
        var row=files.upload(actor,group,task,file("reconcile.txt","synthetic reconcile"));jdbc.update("UPDATE task_attachment SET lifecycle='STAGED' WHERE id=?",row.id);assertTrue(files.reconcile().promotedReady()>=1);
        var corrupt=files.upload(actor,group,task,file("corrupt.txt","synthetic corruption"));jdbc.update("UPDATE task_attachment SET sha256=? WHERE id=?","0".repeat(64),corrupt.id);assertThrows(RuleViolation.class,()->files.download(actor,group,task,corrupt.id));
        assertTrue(files.reconcile().failed()>=1);assertEquals("FAILED",jdbc.queryForObject("SELECT lifecycle FROM task_attachment WHERE id=?",String.class,corrupt.id));
    }
}
