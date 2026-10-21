package vn.deadline.tracker.service;

import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.multipart.MultipartFile;
import vn.deadline.tracker.domain.RuleViolation;import vn.deadline.tracker.persistence.*;
import java.io.*;import java.nio.ByteBuffer;import java.nio.charset.*;import java.nio.file.*;import java.security.*;import java.time.Clock;import java.time.temporal.ChronoUnit;import java.util.*;import java.util.zip.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

@Service @Transactional
public class AttachmentService {
    private static final long FILE_LIMIT=10L*1024*1024, GROUP_LIMIT=200L*1024*1024;
    private static final Set<String> TYPES=Set.of("pdf","png","jpg","jpeg","txt","docx","xlsx");
    private final GroupAccess access;private final TaskRepository tasks;private final TaskAttachmentRepository attachments;private final TaskCommentRepository comments;private final TaskPinRepository pins;private final org.springframework.jdbc.core.JdbcTemplate jdbc;private final Clock clock;private final Path root;private final org.springframework.beans.factory.ObjectProvider<AttachmentScanner> scanners;private final boolean scannerRequired;
    public AttachmentService(GroupAccess access,TaskRepository tasks,TaskAttachmentRepository attachments,TaskCommentRepository comments,TaskPinRepository pins,org.springframework.jdbc.core.JdbcTemplate jdbc,Clock clock,@Value("${app.attachments.root:${java.io.tmpdir}/deadline-tracker-attachments}") String configured,org.springframework.beans.factory.ObjectProvider<AttachmentScanner> scanners,@Value("${app.attachments.scanner-required:false}") boolean scannerRequired)throws IOException{
        this.access=access;this.tasks=tasks;this.attachments=attachments;this.comments=comments;this.pins=pins;this.jdbc=jdbc;this.clock=clock;this.scanners=scanners;this.scannerRequired=scannerRequired;
        Path base=Path.of(configured).toAbsolutePath().normalize();Path work=Path.of(System.getProperty("user.dir")).toRealPath();if(base.startsWith(work))throw new IllegalStateException("Attachment storage must be outside the application workspace");
        Files.createDirectories(base);this.root=base.toRealPath();if(root.startsWith(work)||Files.isSymbolicLink(base))throw new IllegalStateException("Attachment root cannot be a symbolic link");setPrivate(root,true);
    }
    @Transactional(rollbackFor=Exception.class) public TaskAttachmentEntity upload(String actor,String group,String task,MultipartFile file)throws IOException{
        return upload(actor,group,task,file,null);
    }
    @Transactional(rollbackFor=Exception.class) public TaskAttachmentEntity upload(String actor,String group,String task,MultipartFile file,String commentId)throws IOException{
        access.member(actor,group);
        if(commentId!=null&&!commentId.isBlank())comments.findByIdAndTaskId(commentId,task).filter(c->c.hiddenAt==null).orElseThrow(RuleViolation::missing);
        else commentId=null;
        var taskRow=tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);if(file==null||file.isEmpty())throw RuleViolation.invalid("Chọn một tệp không rỗng.");
        if(file.getSize()>FILE_LIMIT)throw RuleViolation.invalid("Tệp vượt quá 10 MiB.");String name=displayName(file.getOriginalFilename());String ext=extension(name);if(!TYPES.contains(ext))throw RuleViolation.invalid("Định dạng tệp không được hỗ trợ.");
        Path stage=Files.createTempFile(root,".upload-",".part");setPrivate(stage,false);long size;String hash;
        try{var digest=MessageDigest.getInstance("SHA-256");size=copyBounded(file.getInputStream(),stage,digest);if(size<1||size>FILE_LIMIT)throw RuleViolation.invalid("Tệp vượt quá 10 MiB.");validate(stage,ext);var scanner=scanners.getIfAvailable();var verdict=scanner==null?AttachmentScanner.Verdict.UNAVAILABLE:scanner.scan(stage);if(verdict==AttachmentScanner.Verdict.REJECTED||scannerRequired&&verdict!=AttachmentScanner.Verdict.CLEAN)throw RuleViolation.invalid("Tệp chưa được chấp nhận bởi chính sách quét của máy chủ.");hash=HexFormat.of().formatHex(digest.digest());}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}catch(Exception e){Files.deleteIfExists(stage);if(e instanceof IOException io)throw io;if(e instanceof RuntimeException runtime)throw runtime;throw new IOException(e);}
        try {
        jdbc.queryForObject("SELECT id FROM project_group WHERE id=? FOR UPDATE",String.class,group);
        long taskCount=jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=?",Long.class,task);
        long groupBytes=jdbc.queryForObject("SELECT COALESCE(SUM(size_bytes),0) FROM task_attachment WHERE group_id=?",Long.class,group);
        if(taskCount>=20||groupBytes+size>GROUP_LIMIT){Files.deleteIfExists(stage);throw RuleViolation.invalid("Đã hết hạn mức tệp của công việc hoặc nhóm.");}
        var row=new TaskAttachmentEntity();row.id=UUID.randomUUID().toString();row.taskId=task;row.commentId=commentId;row.groupId=group;row.uploaderId=actor;row.displayName=name;row.mediaType=mediaType(ext);row.sizeBytes=size;row.sha256=hash;row.storageKey=UUID.randomUUID().toString();row.lifecycle="STAGED";row.createdAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        row=attachments.saveAndFlush(row);Path target=blob(row.storageKey);Files.move(stage,target,ATOMIC_MOVE);row.lifecycle="READY";attachments.flush();return row;
        } finally { Files.deleteIfExists(stage); }
    }
    @Transactional(readOnly=true) public List<TaskAttachmentEntity> list(String actor,String group,String task){access.member(actor,group);tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);return attachments.findByTaskIdOrderByCreatedAtDescIdDesc(task).stream().filter(a->visible(a)).toList();}
    @Transactional(readOnly=true) public List<TaskPinEntity> pins(String actor,String group,String task){access.member(actor,group);tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);return pins.findByTaskIdOrderByCreatedAtAscIdAsc(task);}
    @Transactional public ReconcileResult reconcile() throws IOException{
        int ready=0,failed=0,orphans=0,staged=0;var referenced=new HashSet<String>();
        for(var row:attachments.findAll()){referenced.add(row.storageKey+".blob");if(!"STAGED".equals(row.lifecycle)&&!"READY".equals(row.lifecycle))continue;boolean pending="STAGED".equals(row.lifecycle);if(pending)staged++;Path p=blob(row.storageKey);
            if(Files.isRegularFile(p,NOFOLLOW_LINKS)&&!Files.isSymbolicLink(p)&&Files.size(p)==row.sizeBytes&&hash(p).equals(row.sha256)){row.lifecycle="READY";if(pending)ready++;}
            else{row.lifecycle="FAILED";failed++;pins.findByTaskIdAndAttachmentId(row.taskId,row.id).ifPresent(pins::delete);}
        }
        try(var paths=Files.list(root)){orphans=(int)paths.filter(p->p.getFileName().toString().endsWith(".blob")&&!referenced.contains(p.getFileName().toString())).count();}
        int parts;try(var paths=Files.list(root)){parts=(int)paths.filter(p->p.getFileName().toString().endsWith(".part")).count();}
        return new ReconcileResult(staged,ready,failed,orphans,parts);
    }
    @Transactional(readOnly=true) public Download download(String actor,String group,String task,String id)throws IOException{
        access.member(actor,group);tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);var row=attachments.findByIdAndTaskId(id,task).filter(a->visible(a)).orElseThrow(RuleViolation::missing);Path p=blob(row.storageKey);if(Files.isSymbolicLink(p)||!Files.isRegularFile(p,NOFOLLOW_LINKS)||!p.toRealPath().startsWith(root)||Files.size(p)!=row.sizeBytes||!hash(p).equals(row.sha256))throw RuleViolation.missing();return new Download(row,Files.newInputStream(p));
    }
    public void hide(String actor,String group,String task,String id){
        var g=access.member(actor,group);
        tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);
        var row=attachments.findByIdAndTaskId(id,task).filter(a->group.equals(a.groupId)).orElseThrow(RuleViolation::missing);
        if(!row.uploaderId.equals(actor)&&!g.isOwner(actor))throw RuleViolation.forbidden();
        row.hiddenAt=clock.instant().truncatedTo(ChronoUnit.MICROS);row.hiddenBy=actor;
        pins.findByTaskIdAndAttachmentId(task,id).ifPresent(pins::delete);
    }
    public void pin(String actor,String group,String task,String itemId,boolean attachment){access.member(actor,group);var taskRow=tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);var g=access.member(actor,group);if(!g.isOwner(actor)&&!taskRow.createdBy.equals(actor))throw RuleViolation.forbidden();jdbc.queryForObject("SELECT id FROM task WHERE id=? FOR UPDATE",String.class,task);if(pins.countByTaskId(task)>=5)throw RuleViolation.invalid("Mỗi công việc chỉ được ghim tối đa 5 mục.");
        var pin=new TaskPinEntity();pin.id=UUID.randomUUID().toString();pin.taskId=task;pin.groupId=group;pin.pinnedBy=actor;pin.createdAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        if(attachment){var row=attachments.findByIdAndTaskId(itemId,task).filter(a->visible(a)).orElseThrow(RuleViolation::missing);if(pins.existsByTaskIdAndAttachmentId(task,itemId))return;pin.attachmentId=itemId;}else{var row=comments.findByIdAndTaskId(itemId,task).filter(c->c.hiddenAt==null).orElseThrow(RuleViolation::missing);if(pins.existsByTaskIdAndCommentId(task,itemId))return;pin.commentId=itemId;}pins.save(pin);
    }
    public void unpin(String actor,String group,String task,String itemId,boolean attachment){access.member(actor,group);var taskRow=tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);var g=access.member(actor,group);if(!g.isOwner(actor)&&!taskRow.createdBy.equals(actor))throw RuleViolation.forbidden();(attachment?pins.findByTaskIdAndAttachmentId(task,itemId):pins.findByTaskIdAndCommentId(task,itemId)).ifPresent(pins::delete);}
    @Transactional(readOnly=true) public boolean visible(TaskAttachmentEntity a){return a.hiddenAt==null&&"READY".equals(a.lifecycle)&&(a.commentId==null||comments.findByIdAndTaskId(a.commentId,a.taskId).filter(c->c.hiddenAt==null).isPresent());}
    public record Download(TaskAttachmentEntity metadata,InputStream body){}
    public record ReconcileResult(int staged,int promotedReady,int failed,int orphanBlobs,int temporaryParts){}
    private Path blob(String key){if(key==null||!key.matches("[0-9a-f-]{36}"))throw RuleViolation.missing();return root.resolve(key+".blob").normalize();}
    private String hash(Path p)throws IOException{try(var in=Files.newInputStream(p)){var digest=MessageDigest.getInstance("SHA-256");byte[] b=new byte[8192];for(int n;(n=in.read(b))>=0;)if(n>0)digest.update(b,0,n);return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private void setPrivate(Path p,boolean directory)throws IOException{try{Files.setPosixFilePermissions(p,directory?Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE):Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));}catch(UnsupportedOperationException ignored){/* Windows ACLs are documented in the server runbook. */}}
    private String displayName(String raw){if(raw==null)throw RuleViolation.invalid("Tên tệp không hợp lệ.");String name=raw.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","_").strip();if(name.isBlank()||name.codePointCount(0,name.length())>255)throw RuleViolation.invalid("Tên tệp không hợp lệ.");return name;}
    private String extension(String name){int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);}
    private long copyBounded(InputStream in,Path target,MessageDigest digest)throws IOException{long count=0;try(in;var out=Files.newOutputStream(target,StandardOpenOption.TRUNCATE_EXISTING)){byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))>=0;){if(n==0)continue;count+=n;if(count>FILE_LIMIT)throw RuleViolation.invalid("Tệp vượt quá 10 MiB.");digest.update(buffer,0,n);out.write(buffer,0,n);}}return count;}
    private void validate(Path file,String ext)throws IOException{byte[] head=new byte[16];try(var in=Files.newInputStream(file)){in.read(head);}switch(ext){case "pdf"->{if(!starts(head,new byte[]{'%','P','D','F','-'}))bad();}case "png"->{if(!starts(head,new byte[]{(byte)137,80,78,71,13,10,26,10}))bad();}case "jpg","jpeg"->{if(head[0]!=(byte)0xff||head[1]!=(byte)0xd8)bad();}case "txt"->{var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);try(var reader=new InputStreamReader(Files.newInputStream(file),decoder)){char[] chars=new char[4096];for(int n;(n=reader.read(chars))>=0;)for(int i=0;i<n;i++)if(chars[i]==0)bad();}catch(CharacterCodingException e){bad();}}case "docx","xlsx"->validateOffice(file,ext);default->bad();}}
    private void validateOffice(Path file,String ext)throws IOException{try(ZipFile zip=new ZipFile(file.toFile())){var entries=zip.entries();int count=0;long expanded=0;boolean types=false,content=false;while(entries.hasMoreElements()){var e=entries.nextElement();if(++count>500||e.getName().contains("..")||e.getName().startsWith("/"))bad();if(e.getName().toLowerCase(Locale.ROOT).contains("vbaproject"))bad();if(e.getSize()>0){expanded+=e.getSize();if(expanded>40L*1024*1024||e.getCompressedSize()>0&&e.getSize()/Math.max(1,e.getCompressedSize())>100)bad();}if(e.getName().equals("[Content_Types].xml"))types=true;if(ext.equals("docx")&&e.getName().equals("word/document.xml")||ext.equals("xlsx")&&e.getName().equals("xl/workbook.xml"))content=true;}if(!types||!content)bad();}catch(ZipException e){bad();}}
    private static boolean starts(byte[] bytes,byte[] prefix){return Arrays.equals(Arrays.copyOf(bytes,prefix.length),prefix);}
    private static void bad(){throw RuleViolation.invalid("Nội dung tệp không khớp định dạng được phép.");}
    private String mediaType(String ext){return switch(ext){case "pdf"->"application/pdf";case "png"->"image/png";case "jpg","jpeg"->"image/jpeg";case "txt"->"text/plain; charset=utf-8";case "docx"->"application/vnd.openxmlformats-officedocument.wordprocessingml.document";default->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";};}
}
