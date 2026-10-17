package vn.deadline.tracker.ops;
import org.springframework.stereotype.Component;import org.springframework.context.annotation.Profile;import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import java.util.*;
@Component @Profile("ops") @ConditionalOnProperty(name="app.ops.execute",havingValue="true",matchIfMissing=true)
public class OpsCommand implements CommandLineRunner {
    private final OpsActions actions;private final OpsConsole console;public OpsCommand(OpsActions actions,OpsConsole console){this.actions=actions;this.console=console;}
    public void run(String... args){
        var commands=Arrays.stream(args).filter(a->!a.startsWith("--")).toList();
        if(commands.size()!=3||!commands.get(0).equals("ops"))throw new IllegalArgumentException("Dùng: --spring.profiles.active=ops ops reset-password|anonymize-user <username>");
        if(!console.available())throw new IllegalStateException("Cần console tương tác; không chạy ops qua web hoặc đường ống log.");
        String username=commands.get(2);
        switch(commands.get(1)){
            case "reset-password"->{console.secret(actions.resetPassword(username));console.message("Các phiên cũ đã hủy. Người dùng phải đổi mật khẩu tạm trước khi sử dụng nhóm.");}
            case "anonymize-user"->{String confirmation=console.readLine("Tài khoản sẽ bị vô hiệu hóa; công việc/lịch sử được giữ. Nhập lại tên tài khoản để xác nhận: ");if(!username.equals(confirmation))throw new IllegalArgumentException("Đã hủy: xác nhận không khớp.");actions.anonymize(username);console.message("Đã vô hiệu hóa và đổi định danh tài khoản; không xóa công việc/lịch sử.");}
            default->throw new IllegalArgumentException("Lệnh ops không được hỗ trợ.");
        }
    }
}
