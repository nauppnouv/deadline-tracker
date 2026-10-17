package vn.deadline.tracker.security;
import java.net.URLEncoder;import java.nio.charset.StandardCharsets;
public final class ReturnPath {
    private ReturnPath(){}
    public static String invitation(String path){return path!=null&&path.matches("/invitations/[A-Za-z0-9_-]{43}")?path:null;}
    public static String login(String path,String flag){String next=invitation(path);return "/login?"+flag+(next==null?"":"&next="+URLEncoder.encode(next,StandardCharsets.UTF_8));}
}
