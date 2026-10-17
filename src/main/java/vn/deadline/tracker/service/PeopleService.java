package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.persistence.AccountRepository;import java.util.*;
@Service @Transactional(readOnly=true)
public class PeopleService {
    public record Person(String id,String displayName,boolean owner,boolean active){}
    private final GroupAccess access;private final AccountRepository users;
    public PeopleService(GroupAccess access,AccountRepository users){this.access=access;this.users=users;}
    public List<Person> members(String actor,String groupId){var group=access.member(actor,groupId);return users.findAllById(group.getMembers()).stream().map(u->new Person(u.id,u.displayName,group.isOwner(u.id),u.enabled)).sorted(Comparator.comparing(Person::displayName).thenComparing(Person::id)).toList();}
}
