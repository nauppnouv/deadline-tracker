package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.deadline.tracker.security.TrackerPrincipal;import vn.deadline.tracker.service.NotificationService;
@Controller
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications){this.notifications=notifications;}
    @GetMapping("/notifications") String inbox(@AuthenticationPrincipal TrackerPrincipal actor,@RequestParam(required=false) String before,Model model){var page=notifications.page(actor.id(),before);model.addAttribute("items",page.items());model.addAttribute("nextCursor",page.nextCursor());model.addAttribute("unread",notifications.unread(actor.id()));return "notifications";}
    @GetMapping("/notifications/{id}") String detail(@PathVariable String id,@AuthenticationPrincipal TrackerPrincipal actor,Model model){notifications.view(actor.id(),id);model.addAttribute("notificationId",id);return "notification";}
    @PostMapping("/notifications/{id}/open") String open(@PathVariable String id,@AuthenticationPrincipal TrackerPrincipal actor){return "redirect:"+notifications.open(actor.id(),id);}
    @PostMapping("/notifications/{id}/read") String read(@PathVariable String id,@AuthenticationPrincipal TrackerPrincipal actor){notifications.read(actor.id(),id);return "redirect:/notifications";}
}
