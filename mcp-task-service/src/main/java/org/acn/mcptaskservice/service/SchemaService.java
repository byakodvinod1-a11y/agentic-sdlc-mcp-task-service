package org.acn.mcptaskservice.service;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SchemaService {

    public Map<String, Object> getTaskSchema() {
        return Map.of(
                "specVersion", "2025-06-18",
                "tool", "mcp-schema-tasks",
                "table", "tasks",
                "insertPayloadSchema", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "required", List.of("title"),
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "title", Map.of("type", "string", "maxLength", 140),
                                        "description", Map.of("type", "string", "maxLength", 5000),
                                        "status", Map.of("type", "string", "enum", List.of("OPEN", "IN_PROGRESS", "DONE", "BLOCKED")),
                                        "priority", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH")),
                                        "dueDate", Map.of("type", "string", "format", "date")
                                )
                        )
                ),
                "exampleInsertPayload", List.of(
                        Map.of(
                                "title", "Prepare sprint planning notes",
                                "description", "Collect stories and priorities for next sprint.",
                                "status", "OPEN",
                                "priority", "HIGH",
                                "dueDate", "2026-04-10"
                        )
                )
        );
    }
}