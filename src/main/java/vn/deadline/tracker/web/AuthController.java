package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.deadline.tracker.security.TrackerPrincipal;import vn.deadline.tracker.service.AccountService;import vn.deadline.tracker.domain.RuleViolation;
import jakarta.servlet.http.HttpServletRequest;
@Controller
public class AuthController {
    private final AccountService accounts;public AuthController(AccountService accounts){this.accounts=accounts;}
    @GetMapping("/login") String login(){return "login";}
    @GetMapping("/register") String register(){return "register";}
    @PostMapping("/register") String register(@RequestParam String username,@RequestParam String displayName,@RequestParam String password,Model model,jakarta.servlet.http.HttpServletResponse response){
        try{accounts.register(username,displayName,password);return "redirect:/login?registered";}
        catch(RuleViolation error){response.setStatus(400);model.addAttribute("message",error.getMessage());model.addAttribute("username",username);model.addAttribute("displayName",displayName);return "register";}
    }
    @GetMapping("/account") String account(@AuthenticationPrincipal TrackerPrincipal principal,Model model){var a=accounts.current(principal.id());model.addAttribute("username",a.username);model.addAttribute("displayName",a.displayName);return "account";}
    @GetMapping("/password") String password(){return "password";}
    @PostMapping("/password") String password(@AuthenticationPrincipal TrackerPrincipal principal,@RequestParam String currentPassword,@RequestParam String newPassword,@RequestParam String confirmPassword,HttpServletRequest request,Model model,jakarta.servlet.http.HttpServletResponse response){
        try{if(!newPassword.equals(confirmPassword))throw RuleViolation.invalid("Mật khẩu xác nhận chưa khớp.");accounts.changePassword(principal.id(),currentPassword,newPassword);
            var session=request.getSession(false);if(session!=null)session.invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();return "redirect:/login?changed";
        }catch(RuleViolation error){response.setStatus(400);model.addAttribute("message",error.getMessage());return "password";}
    }
}
