package vn.deadline.tracker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.domain.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class GroupAccess {
    private final GroupRepository groups; private final MemberRepository members; private final AccountRepository accounts;
    public GroupAccess(GroupRepository groups,MemberRepository members,AccountRepository accounts) {this.groups=groups;this.members=members;this.accounts=accounts;}
    public void active(String actor) { if(actor==null || accounts.findById(actor).filter(a->a.enabled).isEmpty()) throw RuleViolation.missing(); }
    public Group member(String actor,String groupId) {
        active(actor);
        if(!members.existsByGroupIdAndUserId(groupId,actor)) throw RuleViolation.missing();
        var row=groups.findById(groupId).orElseThrow(RuleViolation::missing);
        return Group.restore(row.id,row.name,row.description,row.ownerId,members.findByGroupIdOrderByJoinedAtAsc(groupId).stream().map(m->m.userId).collect(Collectors.toSet()));
    }
    public Group owner(String actor,String groupId) {var group=member(actor,groupId);group.requireOwner(actor);return group;}
}
