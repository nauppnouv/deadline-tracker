package vn.deadline.tracker.web;
import java.time.*;import java.time.format.DateTimeFormatter;
public class TimeView {
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public String format(Instant instant){return instant==null?"—":DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE).format(instant);}
    public String status(vn.deadline.tracker.domain.TaskStatus status){return switch(status){case TODO->"Chưa làm";case IN_PROGRESS->"Đang làm";case DONE->"Hoàn thành";};}
}
