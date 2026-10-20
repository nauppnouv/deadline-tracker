package vn.deadline.tracker.ops;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.deadline.tracker.service.AttachmentService;

@Component @Profile("ops") @ConditionalOnProperty(name="app.attachments.reconcile",havingValue="true")
public class AttachmentReconcileCommand implements CommandLineRunner {
    private final AttachmentService files;
    public AttachmentReconcileCommand(AttachmentService files){this.files=files;}
    public void run(String... args)throws Exception {
        var r=files.reconcile();System.out.printf("ATTACHMENT_RECONCILE staged=%d ready=%d failed=%d orphan_blobs=%d temporary_parts=%d%n",r.staged(),r.promotedReady(),r.failed(),r.orphanBlobs(),r.temporaryParts());
    }
}
