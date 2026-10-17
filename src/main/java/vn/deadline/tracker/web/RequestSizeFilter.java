package vn.deadline.tracker.web;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
import org.springframework.stereotype.Component;import org.springframework.core.annotation.Order;import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        if(request.getContentLengthLong()>16384){response.sendError(413);return;}chain.doFilter(request,response);
    }
}
