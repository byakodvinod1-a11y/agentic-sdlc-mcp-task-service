package org.acn.mcptaskservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.acn.mcptaskservice.dto.TaskCreateRequest;
import org.acn.mcptaskservice.dto.TaskInsertResponse;
import org.acn.mcptaskservice.dto.TaskSummaryResponse;
import org.acn.mcptaskservice.service.SchemaService;
import org.acn.mcptaskservice.service.TaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final SchemaService schemaService;
    private final TaskService taskService;
    private final Validator validator;

    public McpController(
            SchemaService schemaService,
            TaskService taskService,
            Validator validator
    ) {
        this.schemaService = schemaService;
        this.taskService = taskService;
        this.validator = validator;
    }

    @GetMapping("/help")
    public Map<String, Object> help() {
        return Map.of(
                "specVersion", "2025-06-18",
                "tool", "mcp-help",
                "service", "mcp-task-service",
                "tools", List.of(
                        Map.of(
                                "name", "mcp-help",
                                "method", "GET",
                                "path", "/mcp/help",
                                "description", "Returns available MCP endpoints"
                        ),
                        Map.of(
                                "name", "mcp-schema-tasks",
                                "method", "GET",
                                "path", "/mcp/schema/tasks",
                                "description", "Returns simplified JSON schema for tasks"
                        ),
                        Map.of(
                                "name", "mcp-tasks",
                                "method", "POST",
                                "path", "/mcp/tasks",
                                "description", "Accepts JSON array of tasks and inserts into DB"
                        ),
                        Map.of(
                                "name", "mcp-tasks-summary",
                                "method", "GET",
                                "path", "/mcp/tasks/summary",
                                "description", "Returns task count summary by status"
                        )
                )
        );
    }

    @GetMapping("/schema/tasks")
    public Map<String, Object> schemaTasks() {
        return schemaService.getTaskSchema();
    }

    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskInsertResponse insertTasks(@RequestBody @Valid List<@Valid TaskCreateRequest> tasks) {
        if (tasks.size() > 5000) {
            return new TaskInsertResponse(tasks.size(), 0, List.of("Too many tasks in one request. Max = 5000"));
        }

        int inserted = taskService.insertTasks(tasks);
        return new TaskInsertResponse(tasks.size(), inserted, List.of());
    }

    @GetMapping("/tasks/summary")
    public TaskSummaryResponse summary() {
        return new TaskSummaryResponse(
                taskService.getTotalCount(),
                taskService.getCountByStatus()
        );
    }

    @PostMapping("/tools/list")
    public Map<String, Object> listTools() {
        return Map.of(
                "specVersion", "2025-06-18",
                "tool", "tools/list",
                "tools", List.of(
                        Map.of("name", "mcp-schema-tasks", "description", "Get task schema"),
                        Map.of("name", "mcp-tasks", "description", "Insert tasks"),
                        Map.of("name", "mcp-tasks-summary", "description", "Get task summary")
                )
        );
    }

    @PostMapping("/tools/call")
    public Object callTool(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");

        if ("mcp-schema-tasks".equals(name)) {
            return schemaService.getTaskSchema();
        }

        if ("mcp-tasks-summary".equals(name)) {
            return Map.of(
                    "specVersion", "2025-06-18",
                    "tool", "mcp-tasks-summary",
                    "total", taskService.getTotalCount(),
                    "byStatus", taskService.getCountByStatus()
            );
        }

        if ("mcp-tasks".equals(name)) {
            Object args = request.get("arguments");
            if (!(args instanceof List<?> rawList)) {
                throw new IllegalArgumentException("arguments must be a list of task objects");
            }

            List<TaskCreateRequest> tasks = rawList.stream()
                    .map(this::mapToTask)
                    .toList();

            if (tasks.size() > 5000) {
                return new TaskInsertResponse(tasks.size(), 0, List.of("Too many tasks in one request. Max = 5000"));
            }

            validateTasks(tasks);

            int inserted = taskService.insertTasks(tasks);
            return new TaskInsertResponse(tasks.size(), inserted, List.of());
        }

        throw new IllegalArgumentException("Unknown tool: " + name);
    }

    private TaskCreateRequest mapToTask(Object item) {
        if (!(item instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("Each argument must be a task object");
        }

        TaskCreateRequest t = new TaskCreateRequest();
        t.setTitle((String) m.get("title"));
        t.setDescription((String) m.get("description"));
        t.setStatus((String) m.get("status"));
        t.setPriority((String) m.get("priority"));
        t.setDueDate((String) m.get("dueDate"));
        return t;
    }

private void validateTasks(List<TaskCreateRequest> tasks) {
    for (int i = 0; i < tasks.size(); i++) {
        final int index = i;

        TaskCreateRequest task = tasks.get(i);
        Set<jakarta.validation.ConstraintViolation<TaskCreateRequest>> violations =
                validator.validate(task);

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(v -> "task[" + index + "]." + v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Validation failed: " + msg);
        }
    }
}
}