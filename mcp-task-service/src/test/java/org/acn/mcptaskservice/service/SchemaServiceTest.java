package org.acn.mcptaskservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaServiceTest {

    private final SchemaService schemaService = new SchemaService();

    @Test
    void getTaskSchema_shouldContainTopLevelFields() {
        Map<String, Object> schema = schemaService.getTaskSchema();

        assertEquals("2025-06-18", schema.get("specVersion"));
        assertEquals("mcp-schema-tasks", schema.get("tool"));
        assertEquals("tasks", schema.get("table"));
        assertTrue(schema.containsKey("insertPayloadSchema"));
        assertTrue(schema.containsKey("exampleInsertPayload"));
    }

    @Test
    void getTaskSchema_shouldContainRequiredTitleField() {
        Map<String, Object> schema = schemaService.getTaskSchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> insertPayloadSchema = (Map<String, Object>) schema.get("insertPayloadSchema");

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) insertPayloadSchema.get("items");

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) items.get("required");

        assertEquals(List.of("title"), required);
    }

    @Test
    void getTaskSchema_shouldContainExpectedEnums() {
        Map<String, Object> schema = schemaService.getTaskSchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> insertPayloadSchema = (Map<String, Object>) schema.get("insertPayloadSchema");

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) insertPayloadSchema.get("items");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) items.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) properties.get("status");

        @SuppressWarnings("unchecked")
        Map<String, Object> priority = (Map<String, Object>) properties.get("priority");

        assertEquals(List.of("OPEN", "IN_PROGRESS", "DONE", "BLOCKED"), status.get("enum"));
        assertEquals(List.of("LOW", "MEDIUM", "HIGH"), priority.get("enum"));
    }

    @Test
    void getTaskSchema_shouldDisallowAdditionalProperties() {
        Map<String, Object> schema = schemaService.getTaskSchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> insertPayloadSchema = (Map<String, Object>) schema.get("insertPayloadSchema");

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) insertPayloadSchema.get("items");

        assertEquals(false, items.get("additionalProperties"));
    }

    @Test
    void getTaskSchema_shouldExposeExamplePayload() {
        Map<String, Object> schema = schemaService.getTaskSchema();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> examples = (List<Map<String, Object>>) schema.get("exampleInsertPayload");

        assertFalse(examples.isEmpty());
        assertEquals("Prepare sprint planning notes", examples.get(0).get("title"));
    }
}