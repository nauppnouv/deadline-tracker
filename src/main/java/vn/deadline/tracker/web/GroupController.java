package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import vn.deadline.tracker.security.TrackerPrincipal;
import vn.deadline.tracker.service.*;import vn.deadline.tracker.domain.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.validation.BindingResult;
@Controller
public class GroupController {
    public record Form(@NotBlank @Size(max=100) String name,@Size(max=1000) String description){}
    private final WorkService work;private final PeopleService people;private final InvitationService invites;
    public GroupController(WorkService work,PeopleService people,InvitationService invites){this.work=work;this.people=people;this.invites=invites;}
    @GetMapping("/") String home(){return "redirect:/groups";}
    @GetMapping("/groups") String groups(@AuthenticationPrincipal TrackerPrincipal actor,Model model){model.addAttribute("groups",work.groups(actor.id()));model.addAttribute("actorId",actor.id());return "groups";}
    @GetMapping("/groups/new") String create(Model model){model.addAttribute("form",new Form("",""));model.addAttribute("editing",false);return "group-form";}
    @PostMapping("/groups") String create(@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") Form form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        model.addAttribute("editing",false);if(errors.hasErrors()){response.setStatus(400);return "group-form";}
        try{return "redirect:/groups/"+work.createGroup(actor.id(),form.name(),form.description()).getId();}catch(RuleViolation e){if(e.getKind()!=RuleViolation.Kind.VALIDATION)throw e;errors.reject("invalid",e.getMessage());response.setStatus(400);return "group-form";}
    }
    @GetMapping("/groups/{group}") String group(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,Model model){
        var g=work.group(actor.id(),group);model.addAttribute("group",g);model.addAttribute("members",people.members(actor.id(),group));model.addAttribute("tasks",work.tasks(actor.id(),group));model.addAttribute("owner",g.isOwner(actor.id()));return "group";
    }
    @GetMapping("/groups/{group}/settings") String settings(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,Model model){var g=work.group(actor.id(),group);g.requireOwner(actor.id());settingsModel(group,actor.id(),model);model.addAttribute("form",new Form(g.getName(),g.getDescription()));return "group-settings";}
    @PostMapping("/groups/{group}/settings") String settings(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") Form form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        work.group(actor.id(),group).requireOwner(actor.id());if(errors.hasErrors()){settingsModel(group,actor.id(),model);response.setStatus(400);return "group-settings";}
        try{work.renameGroup(actor.id(),group,form.name(),form.description());return "redirect:/groups/"+group+"/settings";}catch(RuleViolation e){if(e.getKind()!=RuleViolation.Kind.VALIDATION)throw e;errors.reject("invalid",e.getMessage());settingsModel(group,actor.id(),model);response.setStatus(400);return "group-settings";}
    }
    private void settingsModel(String group,String actor,Model model){model.addAttribute("group",work.group(actor,group));model.addAttribute("invitations",invites.listings(actor,group));}
}
