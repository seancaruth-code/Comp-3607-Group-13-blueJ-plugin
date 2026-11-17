package com.comp3607.telemetry.report;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SampleSessionFixtureTest {
    @Test void suppliedSessionFixtureContainsAllCoreTelemetryCategories() throws Exception {
        try (InputStream stream=getClass().getResourceAsStream("/sample-session.json")) {
            assertNotNull(stream,"sample-session.json should be available as test data");
            String json=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(json.contains("\"attempts\": 5"));
            assertTrue(json.contains("\"successRate\": 0.2"));
            assertTrue(json.contains("\"successful\": false"));
            assertTrue(json.contains("\"eventType\": \"PROJECT\""));
            assertTrue(json.contains("\"eventType\": \"CLASS\""));
            assertTrue(json.contains("\"eventType\": \"EDITOR\""));
            assertTrue(json.contains("\"eventType\": \"EXECUTION\""));
            assertTrue(json.contains("\"errorLine\": 4"));
        }
    }
}
