package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import vn.deadline.tracker.security.TrackerPrincipal;
import vn.deadline.tracker.service.*;import vn.deadline.tracker.domain.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.validation.BindingResult;
@Controller
public class GroupController {
    public record Form(@NotBlank @Size(max=100) String name,@Size(max=1000) String description){}
    public record SettingsForm(@NotBlank @Size(max=100) String name,@Size(max=1000) String description,@NotNull @PositiveOrZero Long version){}
    private final WorkService work;private final PeopleService people;private final InvitationService invites;private final java.time.Clock clock;
    public GroupController(WorkService work,PeopleService people,InvitationService invites,java.time.Clock clock){this.work=work;this.people=people;this.invites=invites;this.clock=clock;}
    @GetMapping("/") String home(){return "redirect:/groups";}
    @GetMapping("/groups") String groups(@AuthenticationPrincipal TrackerPrincipal actor,Model model){model.addAttribute("groups",work.groups(actor.id()));model.addAttribute("actorId",actor.id());return "groups";}
    @GetMapping("/groups/new") String create(Model model){model.addAttribute("form",new Form("",""));model.addAttribute("editing",false);return "group-form";}
    @PostMapping("/groups") String create(@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") Form form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        model.addAttribute("editing",false);if(errors.hasErrors()){response.setStatus(400);return "group-form";}
        try{return "redirect:/groups/"+work.createGroup(actor.id(),form.name(),form.description()).getId();}catch(RuleViolation e){if(e.getKind()!=RuleViolation.Kind.VALIDATION)throw e;errors.reject("invalid",e.getMessage());response.setStatus(400);return "group-form";}
    }
    @GetMapping("/groups/{group}") String group(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,@RequestParam(required=false) String assigneeId,@RequestParam(required=false) TaskStatus status,@RequestParam(defaultValue="ALL") TaskFilter.Due due,@RequestParam(defaultValue="false") boolean mine,Model model){
        var g=work.group(actor.id(),group);var members=people.members(actor.id(),group);var all=work.tasks(actor.id(),group);var now=clock.instant();var filter=new TaskFilter(mine?actor.id():assigneeId,status,due);
        model.addAttribute("group",g);model.addAttribute("members",members);model.addAttribute("names",members.stream().collect(java.util.stream.Collectors.toMap(PeopleService.Person::id,PeopleService.Person::displayName)));
        model.addAttribute("tasks",all.stream().filter(t->filter.matches(t,now)).toList());model.addAttribute("owner",g.isOwner(actor.id()));model.addAttribute("progress",Progress.of(all,now));
        model.addAttribute("now",now);model.addAttribute("filter",filter);model.addAttribute("mine",mine);model.addAttribute("statuses",TaskStatus.values());return "group";
    }
    @GetMapping("/groups/{group}/settings") String settings(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,Model model){var g=work.group(actor.id(),group);g.requireOwner(actor.id());settingsModel(group,actor.id(),model);model.addAttribute("form",new SettingsForm(g.getName(),g.getDescription(),g.getVersion()));return "group-settings";}
    @PostMapping("/groups/{group}/settings") String settings(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") SettingsForm form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        work.group(actor.id(),group).requireOwner(actor.id());if(errors.hasErrors()){settingsModel(group,actor.id(),model);response.setStatus(400);return "group-settings";}
        try{work.renameGroup(actor.id(),group,form.name(),form.description(),form.version());return "redirect:/groups/"+group+"/settings";}
        catch(RuleViolation e){
            if(e.getKind()!=RuleViolation.Kind.VALIDATION&&e.getKind()!=RuleViolation.Kind.CONFLICT)throw e;
            errors.reject("invalid",e.getMessage());settingsModel(group,actor.id(),model);
            boolean stale=e.getKind()==RuleViolation.Kind.CONFLICT;model.addAttribute("stale",stale);response.setStatus(stale?409:400);return "group-settings";
        }catch(org.springframework.dao.OptimisticLockingFailureException e){
            errors.reject("conflict",RuleViolation.conflict().getMessage());settingsModel(group,actor.id(),model);model.addAttribute("stale",true);response.setStatus(409);return "group-settings";
        }
    }
    private void settingsModel(String group,String actor,Model model){model.addAttribute("group",work.group(actor,group));model.addAttribute("invitations",invites.listings(actor,group));model.addAttribute("stale",false);}
}
