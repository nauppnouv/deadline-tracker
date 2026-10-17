package vn.deadline.tracker.web;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice
public class ViewDefaults {@ModelAttribute("time") public TimeView time(){return new TimeView();}}
