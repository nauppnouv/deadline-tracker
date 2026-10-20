package vn.deadline.tracker.service;

import java.nio.file.Path;

/** Optional server-owned scanner. Format validation is always performed separately. */
public interface AttachmentScanner {
    enum Verdict { CLEAN, REJECTED, UNAVAILABLE }
    Verdict scan(Path stagedFile)throws java.io.IOException;
}
