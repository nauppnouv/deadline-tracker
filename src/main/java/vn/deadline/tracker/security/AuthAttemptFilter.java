package vn.deadline.tracker.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
public class AuthAttemptFilter extends OncePerRequestFilter {
    private final RateLimiter limiter;public AuthAttemptFilter(RateLimiter limiter){this.limiter=limiter;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if("POST".equals(request.getMethod())&&(path.equals("/login")||path.equals("/register"))){
            if(!limiter.allow(path.substring(1),request.getRemoteAddr(),request.getParameter("username"))){response.setStatus(429);response.setHeader("Retry-After",path.equals("/login")?"900":"3600");response.setContentType("text/plain;charset=UTF-8");response.getWriter().write("Quá nhiều lần thử. Vui lòng thử lại sau.");return;}
        }
        chain.doFilter(request,response);
    }
}
