package vn.deadline.tracker.security;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.security.core.context.SecurityContextHolder;
import vn.deadline.tracker.service.GroupAccess;import vn.deadline.tracker.domain.RuleViolation;
public class GroupMembershipFilter extends OncePerRequestFilter {
    private final GroupAccess access;public GroupMembershipFilter(GroupAccess access){this.access=access;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        String path=request.getRequestURI().substring(request.getContextPath().length());
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(path.startsWith("/groups/")&&!path.equals("/groups/new")&&auth!=null&&auth.getPrincipal() instanceof TrackerPrincipal actor){
            String group=path.substring("/groups/".length()).split("/",2)[0];
            try{access.member(actor.id(),group);}catch(RuleViolation denied){response.sendError(404);return;}
        }
        chain.doFilter(request,response);
    }
}
