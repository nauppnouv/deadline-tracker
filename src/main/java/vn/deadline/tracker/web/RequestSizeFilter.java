package vn.deadline.tracker.web;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;
import java.nio.charset.StandardCharsets;import java.util.*;
import org.springframework.http.*;import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Component;import org.springframework.core.annotation.Order;import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeFilter extends OncePerRequestFilter {
    private static final int LIMIT=16384;
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if("POST".equals(request.getMethod())&&path.matches("/groups/[^/]+/tasks/[^/]+/attachments")){
            String type=request.getContentType();
            if(type==null||!type.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")){response.sendError(415);return;}
            if(request.getContentLengthLong()>12L*1024*1024){response.sendError(413);return;}
            chain.doFilter(request,response);return;
        }
        if(request.getContentLengthLong()>LIMIT){response.sendError(413);return;}
        // Read at most LIMIT+1 before parameter binding, also for unknown/chunked length.
        byte[] body=request.getInputStream().readNBytes(LIMIT+1);
        if(body.length>LIMIT){response.sendError(413);return;}
        String type=request.getContentType();
        MediaType media=null;
        try{if(type!=null)media=MediaType.parseMediaType(type);}catch(IllegalArgumentException e){response.sendError(415);return;}
        if("POST".equals(request.getMethod())&&path.equals("/api/push/devices")){
            if(media==null||!MediaType.APPLICATION_JSON.equalsTypeAndSubtype(media)){response.sendError(415);return;}
            chain.doFilter(new CachedJson(request,body),response);return;
        }
        if((media!=null&&!MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(media))||(body.length>0&&media==null)){
            response.sendError(415);return;
        }
        if(body.length==0){chain.doFilter(request,response);return;}
        final CachedForm wrapped;
        try{wrapped=new CachedForm(request,body,media);}
        catch(IllegalArgumentException|org.springframework.http.converter.HttpMessageNotReadableException e){response.sendError(400);return;}
        chain.doFilter(wrapped,response);
    }
    private static class CachedJson extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedJson(HttpServletRequest request,byte[] body){super(request);this.body=body;}
        public int getContentLength(){return body.length;}
        public long getContentLengthLong(){return body.length;}
        public ServletInputStream getInputStream(){var input=new ByteArrayInputStream(body);return new ServletInputStream(){
            public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}
            public void setReadListener(ReadListener listener){throw new IllegalStateException("Only synchronous requests are supported");}
        };}
        public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
    }
    private static class CachedForm extends HttpServletRequestWrapper {
        private final byte[] body;
        private final Map<String,String[]> parameters=new LinkedHashMap<>();
        CachedForm(HttpServletRequest request,byte[] body,MediaType type)throws IOException {
            super(request);this.body=body;
            // Container can still supply query parameters; form bytes have been bounded above.
            request.getParameterMap().forEach((key,values)->parameters.put(key,values.clone()));
            var headers=new HttpHeaders();headers.setContentType(type);
            var values=new FormHttpMessageConverter().read(null,new HttpInputMessage(){
                public InputStream getBody(){return new ByteArrayInputStream(body);}
                public HttpHeaders getHeaders(){return headers;}
            });
            values.forEach((key,list)->{
                String[] old=parameters.getOrDefault(key,new String[0]);String[] merged=Arrays.copyOf(old,old.length+list.size());
                for(int i=0;i<list.size();i++)merged[old.length+i]=list.get(i);
                parameters.put(key,merged);
            });
        }
        public String getParameter(String name){var values=parameters.get(name);return values==null||values.length==0?null:values[0];}
        public String[] getParameterValues(String name){var values=parameters.get(name);return values==null?null:values.clone();}
        public Enumeration<String> getParameterNames(){return Collections.enumeration(parameters.keySet());}
        public Map<String,String[]> getParameterMap(){var copy=new LinkedHashMap<String,String[]>();parameters.forEach((k,v)->copy.put(k,v.clone()));return Collections.unmodifiableMap(copy);}
        public int getContentLength(){return body.length;}
        public long getContentLengthLong(){return body.length;}
        public ServletInputStream getInputStream(){
            var input=new ByteArrayInputStream(body);
            return new ServletInputStream(){
                public int read(){return input.read();}
                public boolean isFinished(){return input.available()==0;}
                public boolean isReady(){return true;}
                public void setReadListener(ReadListener listener){throw new IllegalStateException("Only synchronous form requests are supported");}
            };
        }
        public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),getCharacterEncoding()==null?StandardCharsets.UTF_8:java.nio.charset.Charset.forName(getCharacterEncoding())));}
    }
}
