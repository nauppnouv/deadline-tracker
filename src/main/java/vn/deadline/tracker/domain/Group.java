package vn.deadline.tracker.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Group {
    private final String id;
    private final String ownerId;
    private final Set<String> members;
    private String name;
    private String description;
    private long version;
    private Group(String id, String name, String description, String ownerId, Set<String> members) {
        this.id = Inputs.required(id,36,"Mã nhóm");
        this.ownerId = Inputs.required(ownerId,36,"Chủ nhóm");
        if (!members.contains(ownerId)) throw RuleViolation.invalid("Chủ nhóm phải là thành viên.");
        this.name=Inputs.required(name,100,"Tên nhóm");
        this.description=Inputs.optional(description,1000,"Mô tả nhóm");
        this.members=new HashSet<>(members);
    }
    public static Group create(String name, String description, String ownerId) {
        Inputs.required(ownerId,36,"Chủ nhóm");
        return new Group(UUID.randomUUID().toString(),name,description,ownerId,Set.of(ownerId));
    }
    public static Group restore(String id,String name,String description,String ownerId,Set<String> members) {
        return new Group(id,name,description,ownerId,members);
    }
    public static Group restore(String id,String name,String description,String ownerId,Set<String> members,long version) {
        var group=restore(id,name,description,ownerId,members);group.version=version;return group;
    }
    public long getVersion() { return version; }
    public boolean addMember(String userId) { return members.add(Inputs.required(userId,36,"Thành viên")); }
    public void requireMember(String userId) { if (!isMember(userId)) throw RuleViolation.missing(); }
    public void requireOwner(String userId) { requireMember(userId); if (!isOwner(userId)) throw RuleViolation.forbidden(); }
    public void rename(String actor,String name,String description) {
        requireOwner(actor);
        String newName=Inputs.required(name,100,"Tên nhóm");
        String newDescription=Inputs.optional(description,1000,"Mô tả nhóm");
        this.name=newName; this.description=newDescription;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOwnerId() { return ownerId; }
    public boolean isMember(String userId) { return members.contains(userId); }
    public boolean isOwner(String userId) { return ownerId.equals(userId); }
    public Set<String> getMembers() { return Set.copyOf(members); }
}
