package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.persistence.*;import vn.deadline.tracker.domain.*;
import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;import java.security.*;import java.nio.charset.StandardCharsets;
@Service @Transactional
public class InvitationService {
    public record Preview(String groupId,String groupName,String invitedBy,Instant expiresAt,boolean alreadyMember,boolean owner){}
    public record Listing(String id,Instant createdAt,Instant expiresAt,Instant usedAt,Instant revokedAt){}
    private final InvitationRepository rows;private final InvitationStore store;private final GroupAccess access;
    private final AccountRepository users;private final GroupRepository groups;private final Clock clock;private final SecureRandom random=new SecureRandom();
    public InvitationService(InvitationRepository rows,InvitationStore store,GroupAccess access,AccountRepository users,GroupRepository groups,Clock clock){this.rows=rows;this.store=store;this.access=access;this.users=users;this.groups=groups;this.clock=clock;}
    public String issue(String actor,String groupId){
        access.owner(actor,groupId);byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var row=new InvitationEntity();row.id=UUID.randomUUID().toString();row.groupId=groupId;row.tokenHash=hash(token);row.createdBy=actor;row.createdAt=clock.instant().truncatedTo(ChronoUnit.MICROS);row.expiresAt=row.createdAt.plusSeconds(86400);rows.saveAndFlush(row);return token;
    }
    @Transactional(readOnly=true) public Preview preview(String actor,String token){
        access.active(actor);var row=valid(token);var group=groups.findById(row.groupId).orElseThrow(RuleViolation::missing);
        var inviter=users.findById(row.createdBy).orElseThrow(RuleViolation::missing);
        boolean member=access.hasMembership(actor,row.groupId);
        return new Preview(group.id,group.name,inviter.displayName,row.expiresAt,member,group.ownerId.equals(actor));
    }
    public String join(String actor,String token){
        access.active(actor);var row=valid(token);var result=store.claim(row.id,actor);
        if(result==InvitationStore.Claim.UNAVAILABLE)throw RuleViolation.invalid("Lời mời không còn sử dụng được.");return row.groupId;
    }
    public void revoke(String actor,String groupId,String id){
        access.owner(actor,groupId);var row=rows.findById(id).filter(i->i.groupId.equals(groupId)).orElseThrow(RuleViolation::missing);
        if(row.usedAt!=null)throw RuleViolation.invalid("Lời mời đã được sử dụng.");row.revokedAt=clock.instant().truncatedTo(ChronoUnit.MICROS);rows.flush();
    }
    @Transactional(readOnly=true) public List<Listing> listings(String actor,String groupId){access.owner(actor,groupId);return rows.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(i->new Listing(i.id,i.createdAt,i.expiresAt,i.usedAt,i.revokedAt)).toList();}
    private InvitationEntity valid(String token){
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw RuleViolation.missing();
        var row=rows.findByTokenHash(hash(token)).orElseThrow(RuleViolation::missing);
        if(row.usedAt!=null||row.revokedAt!=null||!clock.instant().isBefore(row.expiresAt))throw RuleViolation.invalid("Lời mời đã hết hạn, bị thu hồi hoặc đã được sử dụng.");return row;
    }
    public static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}}
}
