package vn.deadline.tracker.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.deadline.tracker.persistence.AccountRepository;
public class AccountStateFilter extends OncePerRequestFilter {
    private final AccountRepository accounts;public AccountStateFilter(AccountRepository accounts){this.accounts=accounts;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null&&auth.getPrincipal() instanceof TrackerPrincipal principal){
            var account=accounts.findById(principal.id());
            if(account.isEmpty()||!account.get().enabled||account.get().version!=principal.version()){
                var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();
                if(request.getRequestURI().substring(request.getContextPath().length()).startsWith("/groups/"))response.sendError(404);else response.sendRedirect(request.getContextPath()+"/login");return;
            }
            String path=request.getRequestURI().substring(request.getContextPath().length());
            if(account.get().mustChangePassword&&!path.equals("/password")&&!path.equals("/logout")&&!path.startsWith("/css/")){
                response.sendRedirect(request.getContextPath()+"/password");return;
            }
        }
        chain.doFilter(request,response);
    }
}
