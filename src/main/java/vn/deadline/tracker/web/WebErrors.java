package vn.deadline.tracker.web;
import org.springframework.web.bind.annotation.*;import org.springframework.web.servlet.ModelAndView;
import org.springframework.dao.OptimisticLockingFailureException;import org.springframework.dao.DataIntegrityViolationException;
import vn.deadline.tracker.domain.RuleViolation;
import java.util.UUID;
@ControllerAdvice
public class WebErrors {
    @ExceptionHandler(RuleViolation.class) ModelAndView rule(RuleViolation error){return page(switch(error.getKind()){case VALIDATION->400;case NOT_FOUND->404;case FORBIDDEN->403;case CONFLICT->409;},error.getMessage());}
    @ExceptionHandler(OptimisticLockingFailureException.class) ModelAndView conflict(Exception error){return page(409,"Dữ liệu đã thay đổi. Vui lòng tải lại.");}
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class}) ModelAndView invalid(Exception error){return page(400,"Thông tin gửi lên không hợp lệ hoặc còn thiếu.");}
    @ExceptionHandler(DataIntegrityViolationException.class) ModelAndView integrity(Exception error){return page(409,"Không thể lưu thay đổi. Vui lòng tải lại và kiểm tra dữ liệu.");}
    @ExceptionHandler(Exception.class) ModelAndView unknown(Exception error){
        String ref=UUID.randomUUID().toString();org.slf4j.LoggerFactory.getLogger(WebErrors.class).error("Request failed reference={} type={}",ref,error.getClass().getSimpleName());
        return page(500,"Có lỗi hệ thống. Mã tham chiếu: "+ref);
    }
    private ModelAndView page(int status,String message){var view=new ModelAndView("error");view.setStatus(org.springframework.http.HttpStatus.valueOf(status));view.addObject("message",message);view.addObject("status",status);return view;}
}
