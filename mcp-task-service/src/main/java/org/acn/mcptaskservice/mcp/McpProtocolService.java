package org.acn.mcptaskservice.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.acn.mcptaskservice.dto.TaskCreateRequest;
import org.acn.mcptaskservice.service.SchemaService;
import org.acn.mcptaskservice.service.TaskService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class McpProtocolService {

    public static final String SPEC_VERSION = "2025-06-18";

    private final SchemaService schemaService;
    private final TaskService taskService;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public McpProtocolService(
            SchemaService schemaService,
            TaskService taskService,
            Validator validator,
            ObjectMapper objectMapper
    ) {
        this.schemaService = schemaService;
        this.taskService = taskService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> initialize(Map<String, Object> params) {
        String requestedVersion = stringValue(params.get("protocolVersion"));
        if (requestedVersion == null || requestedVersion.isBlank()) {
            throw new McpProtocolException(-32602, "initialize.params.protocolVersion is required");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", SPEC_VERSION);
        result.put("capabilities", Map.of(
                "tools", Map.of("listChanged", false)
        ));
        result.put("serverInfo", Map.of(
                "name", "mcp-task-service",
                "title", "MCP Task Service",
                "version", "1.0.0"
        ));
        result.put("instructions", "Call tools/list first. Use mcp_schema_tasks to inspect the insert schema, mcp_tasks to insert task batches, and mcp_tasks_summary to validate totals.");
        return result;
    }

    public Map<String, Object> listTools() {
        return Map.of(
                "tools", List.of(
                        Map.of(
                                "name", "mcp_schema_tasks",
                                "title", "Task Schema",
                                "description", "Returns the tasks table schema and an example insert payload.",
                                "inputSchema", emptyInputSchema(),
                                "annotations", Map.of(
                                        "title", "Task Schema",
                                        "readOnlyHint", true,
                                        "destructiveHint", false,
                                        "idempotentHint", true,
                                        "openWorldHint", false
                                )
                        ),
                        Map.of(
                                "name", "mcp_tasks",
                                "title", "Insert Tasks",
                                "description", "Accepts a batch of task objects and inserts them into the database.",
                                "inputSchema", taskToolInputSchema(),
                                "outputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "requested", Map.of("type", "integer"),
                                                "inserted", Map.of("type", "integer"),
                                                "errors", Map.of("type", "integer"),
                                                "errorSamples", Map.of(
                                                        "type", "array",
                                                        "items", Map.of("type", "string")
                                                )
                                        ),
                                        "required", List.of("requested", "inserted", "errors", "errorSamples")
                                ),
                                "annotations", Map.of(
                                        "title", "Insert Tasks",
                                        "readOnlyHint", false,
                                        "destructiveHint", false,
                                        "idempotentHint", false,
                                        "openWorldHint", false
                                )
                        ),
                        Map.of(
                                "name", "mcp_tasks_summary",
                                "title", "Task Summary",
                                "description", "Returns task totals and counts by status.",
                                "inputSchema", emptyInputSchema(),
                                "outputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "total", Map.of("type", "integer"),
                                                "byStatus", Map.of("type", "object")
                                        ),
                                        "required", List.of("total", "byStatus")
                                ),
                                "annotations", Map.of(
                                        "title", "Task Summary",
                                        "readOnlyHint", true,
                                        "destructiveHint", false,
                                        "idempotentHint", true,
                                        "openWorldHint", false
                                )
                        )
                )
        );
    }

    public Map<String, Object> callTool(Map<String, Object> params) {
        String name = stringValue(params.get("name"));
        if (name == null || name.isBlank()) {
            throw new McpProtocolException(-32602, "tools/call.params.name is required");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> rawArgs
                ? (Map<String, Object>) rawArgs
                : Map.of();

        return switch (name) {
            case "mcp_schema_tasks" -> {
                Map<String, Object> schema = schemaService.getTaskSchema();
                yield successResult(schema, toCompactJson(schema));
            }
            case "mcp_tasks_summary" -> {
                Map<String, Object> summary = Map.of(
                        "total", taskService.getTotalCount(),
                        "byStatus", taskService.getCountByStatus()
                );
                yield successResult(summary, toCompactJson(summary));
            }
            case "mcp_tasks" -> callInsertTasks(arguments);
            default -> throw new McpProtocolException(-32601, "Unknown tool: " + name);
        };
    }

private Map<String, Object> callInsertTasks(Map<String, Object> arguments) {
    Object tasksObj = arguments.get("tasks");
    if (!(tasksObj instanceof List<?> rawTasks)) {
        return toolErrorResult("arguments.tasks must be an array of task objects");
    }

    List<TaskCreateRequest> tasks = rawTasks.stream()
            .map(this::mapToTask)
            .toList();

    if (tasks.size() > 5000) {
        return toolErrorResult("Too many tasks in one request. Max = 5000");
    }

    try {
        validateTasks(tasks);

        int inserted = taskService.insertTasks(tasks);

        Map<String, Object> structured = Map.of(
                "requested", tasks.size(),
                "inserted", inserted,
                "errors", tasks.size() - inserted,
                "errorSamples", List.of()
        );

        return successResult(structured, toCompactJson(structured));

      } catch (IllegalArgumentException ex) {
        return toolErrorResult(ex.getMessage());

    } catch (DataAccessException ex) {
        ex.printStackTrace();
        return toolErrorResult("Database error: " + ex.getMostSpecificCause().getMessage());

    } catch (Exception ex) {
        ex.printStackTrace();
        return toolErrorResult("Insert failed: " + ex.getMessage());
    }
}

    private Map<String, Object> emptyInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false
        );
    }

    private Map<String, Object> taskToolInputSchema() {
        @SuppressWarnings("unchecked")
        Map<String, Object> insertPayloadSchema = (Map<String, Object>) schemaService.getTaskSchema().get("insertPayloadSchema");
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "tasks", insertPayloadSchema
                ),
                "required", List.of("tasks"),
                "additionalProperties", false
        );
    }

    private Map<String, Object> successResult(Map<String, Object> structuredContent, String text) {
        return Map.of(
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", text
                        )
                ),
                "structuredContent", structuredContent,
                "isError", false
        );
    }

    private Map<String, Object> toolErrorResult(String message) {
        return Map.of(
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", message
                        )
                ),
                "structuredContent", Map.of(
                        "error", message
                ),
                "isError", true
        );
    }

    private TaskCreateRequest mapToTask(Object item) {
        if (!(item instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Each task must be a JSON object");
        }

        TaskCreateRequest task = new TaskCreateRequest();
        task.setTitle(stringValue(map.get("title")));
        task.setDescription(stringValue(map.get("description")));
        task.setStatus(stringValue(map.get("status")));
        task.setPriority(stringValue(map.get("priority")));
        task.setDueDate(stringValue(map.get("dueDate")));
        return task;
    }

    private void validateTasks(List<TaskCreateRequest> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            int index = i;
            Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(tasks.get(i));
            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                        .map(v -> "task[" + index + "]." + v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Validation failed: " + message);
            }
        }
    }

    private String stringValue(Object value) {
        return value instanceof String s ? s : null;
    }

    private String toCompactJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
