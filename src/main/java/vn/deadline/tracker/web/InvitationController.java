package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import vn.deadline.tracker.security.TrackerPrincipal;import vn.deadline.tracker.service.InvitationService;
@Controller
public class InvitationController {
    private final InvitationService invites;public InvitationController(InvitationService invites){this.invites=invites;}
    @PostMapping("/groups/{group}/invitations") String issue(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor){return "redirect:/invitations/"+invites.issue(actor.id(),group);}
    @GetMapping("/invitations/{token}") String preview(@PathVariable String token,@AuthenticationPrincipal TrackerPrincipal actor,Model model){model.addAttribute("invite",invites.preview(actor.id(),token));model.addAttribute("invitationPath","/invitations/"+token);return "invitation";}
    @PostMapping("/invitations/{token}") String join(@PathVariable String token,@AuthenticationPrincipal TrackerPrincipal actor){return "redirect:/groups/"+invites.join(actor.id(),token);}
    @PostMapping("/groups/{group}/invitations/{invite}/revoke") String revoke(@PathVariable String group,@PathVariable String invite,@AuthenticationPrincipal TrackerPrincipal actor){invites.revoke(actor.id(),group,invite);return "redirect:/groups/"+group+"/settings";}
}
