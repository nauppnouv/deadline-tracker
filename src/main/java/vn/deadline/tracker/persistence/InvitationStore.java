package vn.deadline.tracker.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.deadline.tracker.domain.RuleViolation;
import java.time.Clock;
import java.sql.Timestamp;
import java.util.UUID;

/** Atomic persistence primitive. Web callers must authenticate and resolve the secret token first. */
@Repository
public class InvitationStore {
    public enum Claim { JOINED, ALREADY_MEMBER, UNAVAILABLE }
    private final InvitationRepository invitations; private final MemberRepository members; private final JdbcTemplate jdbc; private final Clock clock;
    public InvitationStore(InvitationRepository invitations,MemberRepository members,JdbcTemplate jdbc,Clock clock) {this.invitations=invitations;this.members=members;this.jdbc=jdbc;this.clock=clock;}
    @Transactional
    public Claim claim(String invitationId,String actor) {
        var row=invitations.findById(invitationId).orElseThrow(RuleViolation::missing);
        if(members.existsByGroupIdAndUserId(row.groupId,actor)) return Claim.ALREADY_MEMBER;
        var now=Timestamp.from(clock.instant());
        int count=jdbc.update("UPDATE invitation SET used_at=?, used_by=?, version=version+1 WHERE id=? AND used_at IS NULL AND revoked_at IS NULL AND expires_at>?",now,actor,invitationId,now);
        if(count==0) return Claim.UNAVAILABLE;
        jdbc.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES(?,?,?,'MEMBER',?)",UUID.randomUUID().toString(),row.groupId,actor,now);
        return Claim.JOINED;
    }
}
