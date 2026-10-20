package vn.deadline.tracker.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttachmentReconciler {
    private final AttachmentService files;
    public AttachmentReconciler(AttachmentService files){this.files=files;}
    @Scheduled(initialDelayString="${app.attachments.reconcile-delay-ms:60000}",fixedDelayString="${app.attachments.reconcile-interval-ms:300000}")
    public void scheduled() {
        try {
            var result=files.reconcile();
            if(result.failed()>0||result.orphanBlobs()>0||result.temporaryParts()>0)
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Attachment reconciliation failed={} orphan_blobs={} temporary_parts={}",result.failed(),result.orphanBlobs(),result.temporaryParts());
        } catch(Exception error){org.slf4j.LoggerFactory.getLogger(getClass()).error("Attachment reconciliation failed type={}",error.getClass().getSimpleName());}
    }
}
