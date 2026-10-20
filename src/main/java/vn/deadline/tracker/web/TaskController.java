package vn.deadline.tracker.web;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.validation.BindingResult;
import org.springframework.format.annotation.DateTimeFormat;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
import vn.deadline.tracker.security.TrackerPrincipal;import vn.deadline.tracker.service.*;import vn.deadline.tracker.domain.*;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.stream.Collectors;
@Controller
public class TaskController {
    public record Form(
        @NotBlank(message="Vui lòng nhập tên công việc.") @Size(max=150,message="Tên công việc tối đa 150 ký tự.") String title,
        @Size(max=2000,message="Mô tả tối đa 2.000 ký tự.") String description,
        @NotBlank(message="Vui lòng chọn người phụ trách.") @Size(max=36) String assigneeId,
        @NotNull(message="Vui lòng chọn hạn nộp.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
        @NotNull @PositiveOrZero Long version){}
    private final WorkService work;private final PeopleService people;private final CommentService comments;private final AttachmentService attachments;private final Clock clock;
    public TaskController(WorkService work,PeopleService people,CommentService comments,AttachmentService attachments,Clock clock){this.work=work;this.people=people;this.comments=comments;this.attachments=attachments;this.clock=clock;}
    @GetMapping("/groups/{group}/tasks/new") String create(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,Model model){
        formModel(actor.id(),group,null,model);model.addAttribute("form",new Form("","",actor.id(),LocalDateTime.ofInstant(clock.instant().plusSeconds(86400),TimeView.ZONE).truncatedTo(ChronoUnit.MINUTES),0L));return "task-form";
    }
    @PostMapping("/groups/{group}/tasks") String create(@PathVariable String group,@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") Form form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        formModel(actor.id(),group,null,model);if(errors.hasErrors()){response.setStatus(400);return "task-form";}
        try{var task=work.createTask(actor.id(),group,form.title(),form.description(),form.assigneeId(),instant(form.dueAt()));return redirect(task);}
        catch(RuleViolation e){return formError(e,errors,model,response);}
    }
    @GetMapping("/groups/{group}/tasks/{task}") String detail(@PathVariable String group,@PathVariable String task,@RequestParam(defaultValue="0") int page,@AuthenticationPrincipal TrackerPrincipal actor,Model model){
        var item=work.task(actor.id(),group,task);var g=work.group(actor.id(),group);var members=people.members(actor.id(),group);
        model.addAttribute("group",g);model.addAttribute("task",item);model.addAttribute("names",members.stream().collect(Collectors.toMap(PeopleService.Person::id,PeopleService.Person::displayName)));
        model.addAttribute("actorId",actor.id());model.addAttribute("members",members);model.addAttribute("mentionSelections",comments.mentionIds(actor.id(),group,task));
        model.addAttribute("timeline",comments.list(actor.id(),group,task,page));
        model.addAttribute("attachments",attachments.list(actor.id(),group,task));
        model.addAttribute("pins",attachments.pins(actor.id(),group,task));
        model.addAttribute("canEdit",g.isOwner(actor.id())||item.getCreatedBy().equals(actor.id()));model.addAttribute("canStatus",item.getAssigneeId().equals(actor.id()));
        model.addAttribute("statuses",TaskStatus.values());model.addAttribute("now",clock.instant());return "task";
    }
    @PostMapping("/groups/{group}/tasks/{task}/comments") String comment(@PathVariable String group,@PathVariable String task,@AuthenticationPrincipal TrackerPrincipal actor,@RequestParam String body,@RequestParam(required=false) java.util.List<String> mentionIds){
        comments.create(actor.id(),group,task,body,mentionIds);return "redirect:/groups/"+group+"/tasks/"+task;
    }
    @PostMapping("/groups/{group}/tasks/{task}/comments/{comment}/edit") String editComment(@PathVariable String group,@PathVariable String task,@PathVariable String comment,@AuthenticationPrincipal TrackerPrincipal actor,@RequestParam String body,@RequestParam long version,@RequestParam(required=false) java.util.List<String> mentionIds,@RequestParam(defaultValue="false") boolean updateMentions){
        comments.edit(actor.id(),group,task,comment,body,version,updateMentions&&mentionIds==null?java.util.List.of():mentionIds);return "redirect:/groups/"+group+"/tasks/"+task;
    }
    @PostMapping("/groups/{group}/tasks/{task}/comments/{comment}/hide") String hideComment(@PathVariable String group,@PathVariable String task,@PathVariable String comment,@AuthenticationPrincipal TrackerPrincipal actor){
        comments.hide(actor.id(),group,task,comment);return "redirect:/groups/"+group+"/tasks/"+task;
    }
    @GetMapping("/groups/{group}/tasks/{task}/comments/{comment}/revisions") String revisions(@PathVariable String group,@PathVariable String task,@PathVariable String comment,@AuthenticationPrincipal TrackerPrincipal actor,Model model){model.addAttribute("revisions",comments.revisions(actor.id(),group,task,comment));model.addAttribute("taskPath","/groups/"+group+"/tasks/"+task);return "comment-revisions";}
    @GetMapping("/groups/{group}/tasks/{task}/edit") String edit(@PathVariable String group,@PathVariable String task,@AuthenticationPrincipal TrackerPrincipal actor,Model model){
        var item=work.task(actor.id(),group,task);requireEditor(actor.id(),group,item);formModel(actor.id(),group,item,model);
        model.addAttribute("form",new Form(item.getTitle(),item.getDescription(),item.getAssigneeId(),LocalDateTime.ofInstant(item.getDueAt(),TimeView.ZONE).truncatedTo(ChronoUnit.MINUTES),item.getVersion()));return "task-form";
    }
    @PostMapping("/groups/{group}/tasks/{task}/edit") String edit(@PathVariable String group,@PathVariable String task,@AuthenticationPrincipal TrackerPrincipal actor,@Valid @ModelAttribute("form") Form form,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        var item=work.task(actor.id(),group,task);requireEditor(actor.id(),group,item);formModel(actor.id(),group,item,model);
        if(errors.hasErrors()){response.setStatus(400);return "task-form";}
        try{return redirect(work.editTask(actor.id(),group,task,form.title(),form.description(),form.assigneeId(),instant(form.dueAt()),form.version()));}
        catch(RuleViolation e){return formError(e,errors,model,response);}
    }
    @PostMapping("/groups/{group}/tasks/{task}/status") String status(@PathVariable String group,@PathVariable String task,@AuthenticationPrincipal TrackerPrincipal actor,@RequestParam TaskStatus status,@RequestParam long version){
        if(version<0)throw RuleViolation.invalid("Phiên bản dữ liệu không hợp lệ.");return redirect(work.changeStatus(actor.id(),group,task,status,version));
    }
    private void requireEditor(String actor,String group,Task item){var g=work.group(actor,group);if(!g.isOwner(actor)&&!item.getCreatedBy().equals(actor))throw RuleViolation.forbidden();}
    private void formModel(String actor,String group,Task task,Model model){model.addAttribute("group",work.group(actor,group));model.addAttribute("members",people.members(actor,group));model.addAttribute("task",task);model.addAttribute("editing",task!=null);model.addAttribute("completed",task!=null&&task.getStatus()==TaskStatus.DONE);model.addAttribute("stale",false);}
    private String formError(RuleViolation error,BindingResult errors,Model model,jakarta.servlet.http.HttpServletResponse response){
        if(error.getKind()==RuleViolation.Kind.CONFLICT){response.setStatus(409);model.addAttribute("stale",true);errors.reject("conflict",error.getMessage());return "task-form";}
        if(error.getKind()!=RuleViolation.Kind.VALIDATION)throw error;response.setStatus(400);
        if(error.getMessage().startsWith("Hạn nộp"))errors.rejectValue("dueAt","future",error.getMessage());
        else if(error.getMessage().startsWith("Người phụ trách"))errors.rejectValue("assigneeId","member",error.getMessage());
        else errors.reject("invalid",error.getMessage());return "task-form";
    }
    private Instant instant(LocalDateTime value){return value.atZone(TimeView.ZONE).toInstant();}
    private String redirect(Task task){return "redirect:/groups/"+task.getGroupId()+"/tasks/"+task.getId();}
}
