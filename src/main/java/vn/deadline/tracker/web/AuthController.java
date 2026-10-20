package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.deadline.tracker.security.TrackerPrincipal;import vn.deadline.tracker.service.AccountService;import vn.deadline.tracker.service.ReminderPreferenceService;import vn.deadline.tracker.domain.RuleViolation;
import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpServletResponse;
@Controller
public class AuthController {
    private final AccountService accounts;private final ReminderPreferenceService reminders;private final vn.deadline.tracker.service.EventPreferenceService events;public AuthController(AccountService accounts,ReminderPreferenceService reminders,vn.deadline.tracker.service.EventPreferenceService events){this.accounts=accounts;this.reminders=reminders;this.events=events;}
    @GetMapping("/login") String login(@RequestParam(required=false) String next,Model model){model.addAttribute("next",vn.deadline.tracker.security.ReturnPath.invitation(next));return "login";}
    @GetMapping("/register") String register(@RequestParam(required=false) String next,Model model){model.addAttribute("next",vn.deadline.tracker.security.ReturnPath.invitation(next));return "register";}
    @PostMapping("/register") String register(@RequestParam String username,@RequestParam String displayName,@RequestParam String password,@RequestParam(required=false) String next,Model model,jakarta.servlet.http.HttpServletResponse response){
        try{accounts.register(username,displayName,password);return "redirect:"+vn.deadline.tracker.security.ReturnPath.login(next,"registered");}
        catch(RuleViolation error){response.setStatus(400);model.addAttribute("message",error.getMessage());model.addAttribute("username",username);model.addAttribute("displayName",displayName);model.addAttribute("next",vn.deadline.tracker.security.ReturnPath.invitation(next));return "register";}
    }
    @GetMapping("/account") String account(@AuthenticationPrincipal TrackerPrincipal principal,Model model){var a=accounts.current(principal.id());var p=reminders.get(principal.id());model.addAttribute("username",a.username);model.addAttribute("displayName",a.displayName);model.addAttribute("events",events.get(principal.id()));model.addAttribute("reminders",p);return "account";}
    @PostMapping("/account/reminders") String reminderPreferences(@AuthenticationPrincipal TrackerPrincipal principal,@RequestParam(defaultValue="false") boolean enabled,@RequestParam(defaultValue="false") boolean twentyFourHours,@RequestParam(defaultValue="false") boolean oneHour,HttpServletResponse response,Model model){try{reminders.update(principal.id(),enabled,twentyFourHours,oneHour);return "redirect:/account?reminders-updated";}catch(RuleViolation error){var a=accounts.current(principal.id());model.addAttribute("username",a.username);model.addAttribute("displayName",a.displayName);model.addAttribute("events",events.get(principal.id()));model.addAttribute("reminders",reminders.get(principal.id()));model.addAttribute("message",error.getMessage());response.setStatus(400);return "account";}}
    @PostMapping("/account/events") String eventPreferences(@AuthenticationPrincipal TrackerPrincipal principal,@RequestParam(defaultValue="false") boolean assignments,@RequestParam(defaultValue="false") boolean comments,@RequestParam(defaultValue="false") boolean mentions){events.update(principal.id(),assignments,comments,mentions);return "redirect:/account?events-updated";}
    @GetMapping("/password") String password(){return "password";}
    @PostMapping("/password") String password(@AuthenticationPrincipal TrackerPrincipal principal,@RequestParam String currentPassword,@RequestParam String newPassword,@RequestParam String confirmPassword,HttpServletRequest request,Model model,jakarta.servlet.http.HttpServletResponse response){
        try{if(!newPassword.equals(confirmPassword))throw RuleViolation.invalid("Mật khẩu xác nhận chưa khớp.");accounts.changePassword(principal.id(),currentPassword,newPassword);
            var session=request.getSession(false);if(session!=null)session.invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();return "redirect:/login?changed";
        }catch(RuleViolation error){response.setStatus(400);model.addAttribute("message",error.getMessage());return "password";}
    }
}
