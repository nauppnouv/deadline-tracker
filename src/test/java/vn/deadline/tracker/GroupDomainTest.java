package vn.deadline.tracker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.deadline.tracker.domain.Group;
import vn.deadline.tracker.domain.RuleViolation;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GroupDomainTest {
    @Test @DisplayName("US-01 BR-14/15 create group and owner atomically in domain")
    void createAndIndependentIdentities() {
        var a = Group.create("  Do an  ", null, "owner");
        var b = Group.create("Do an", "description", "owner");
        assertEquals("Do an", a.getName());
        assertEquals("", a.getDescription());
        assertTrue(a.isOwner("owner"));
        assertTrue(a.isMember("owner"));
        assertNotEquals(a.getId(), b.getId());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n"})
    @DisplayName("US-01 BR-15 invalid group name rejected")
    void invalidName(String value) { assertThrows(RuleViolation.class, () -> Group.create(value,"","owner")); }
    @Test @DisplayName("US-01 BR-15 field limits and missing owner")
    void limits() {
        assertThrows(RuleViolation.class, () -> Group.create("x".repeat(101),"","owner"));
        assertThrows(RuleViolation.class, () -> Group.create("name","x".repeat(1001),"owner"));
        assertThrows(RuleViolation.class, () -> Group.create("name","",null));
    }
    @Test @DisplayName("US-02 BR-03/14 membership uses identity and never duplicates")
    void membersAreIdentityScoped() {
        var a=Group.create("A","","user-a"); var b=Group.create("B","","user-b");
        assertTrue(a.addMember("user-b"));
        assertFalse(a.addMember("user-b"));
        assertEquals(2,a.getMembers().size());
        assertFalse(a.isOwner("user-b")); assertTrue(b.isOwner("user-b"));
        assertFalse(b.isMember("user-a"));
        assertThrows(UnsupportedOperationException.class, () -> a.getMembers().add("outsider"));
    }
    @Test @DisplayName("US-01 owner-only rename and outsider isolation")
    void renamePermission() {
        var a=Group.create("A","","owner"); a.addMember("member");
        assertThrows(RuleViolation.class, () -> a.rename("member","B",""));
        assertThrows(RuleViolation.class, () -> a.requireMember("outsider"));
        a.rename("owner","  B  ","description"); assertEquals("B",a.getName());
        assertThrows(RuleViolation.class, () -> Group.restore("id","A","","owner",Set.of("member")));
    }
}
