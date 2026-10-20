package vn.deadline.tracker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.deadline.tracker.domain.Inputs;
import vn.deadline.tracker.domain.RuleViolation;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @ParameterizedTest @ValueSource(ints = {8, 14, 15, 128})
    void acceptsAsciiPasswordsWithinBounds(int length) {
        assertDoesNotThrow(() -> Inputs.password("a".repeat(length)));
    }

    @ParameterizedTest @ValueSource(ints = {7, 129})
    void rejectsAsciiPasswordsOutsideBounds(int length) {
        assertThrows(RuleViolation.class, () -> Inputs.password("a".repeat(length)));
    }

    @ParameterizedTest @ValueSource(ints = {8, 128})
    void acceptsUnicodeCodePointBounds(int length) {
        assertDoesNotThrow(() -> Inputs.password("🔒".repeat(length)));
    }

    @ParameterizedTest @ValueSource(ints = {7, 129})
    void rejectsUnicodeOutsideCodePointBounds(int length) {
        assertThrows(RuleViolation.class, () -> Inputs.password("🔒".repeat(length)));
    }

    @Test void rejectsNullPassword() {
        assertThrows(RuleViolation.class, () -> Inputs.password(null));
    }

    @Test void rejectsEmptyPassword() {
        assertThrows(RuleViolation.class, () -> Inputs.password(""));
    }

    @Test void acceptsEightCharactersIncludingEdgeSpaces() {
        assertDoesNotThrow(() -> Inputs.password(" abcdef "));
    }
}
