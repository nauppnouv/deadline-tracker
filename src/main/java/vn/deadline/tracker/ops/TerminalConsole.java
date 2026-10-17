package vn.deadline.tracker.ops;
import org.springframework.stereotype.Component;import org.springframework.context.annotation.Profile;
@Component @Profile("ops")
public class TerminalConsole implements OpsConsole {
    public boolean available(){return System.console()!=null;}
    public String readLine(String prompt){if(!available())throw new IllegalStateException("Cần terminal tương tác.");return System.console().readLine("%s",prompt);}
    public void secret(String value){if(!available())throw new IllegalStateException("Không xuất mật khẩu ra log.");System.console().printf("Mật khẩu tạm (chỉ hiện tại console): %s%n",value);}
    public void message(String value){if(available())System.console().printf("%s%n",value);}
}
