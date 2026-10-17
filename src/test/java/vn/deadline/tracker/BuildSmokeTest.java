package vn.deadline.tracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildSmokeTest {
    @Test
    void buildUsesRequiredLtsRuntime() {
        assertEquals(25, Runtime.version().feature(), "Build with JDK 25 LTS; do not change system Java");
    }
}
