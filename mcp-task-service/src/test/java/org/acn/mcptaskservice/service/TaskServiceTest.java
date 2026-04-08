package org.acn.mcptaskservice.service;

import org.acn.mcptaskservice.dto.TaskCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private JdbcOperations jdbcOperations;

    @InjectMocks
    private TaskService taskService;

    private TaskCreateRequest task(
            String title,
            String description,
            String status,
            String priority,
            String dueDate
    ) {
        TaskCreateRequest t = new TaskCreateRequest();
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setDueDate(dueDate);
        return t;
    }

    @Test
    void insertTasks_shouldInsertAllValidTasks() {
        List<TaskCreateRequest> tasks = List.of(
                task("Task A", "Desc A", "OPEN", "HIGH", "2026-04-10"),
                task("Task B", "Desc B", "DONE", "LOW", null)
        );

        when(jdbcOperations.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1, 1});

        int inserted = taskService.insertTasks(tasks);

        assertEquals(2, inserted);
        verify(jdbcOperations).batchUpdate(anyString(), anyList());
    }

    @Test
    void insertTasks_shouldHandleBlankDueDateAsNull() {
        List<TaskCreateRequest> tasks = List.of(
                task("Task A", "Desc A", "OPEN", "HIGH", " ")
        );

        when(jdbcOperations.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1});

        int inserted = taskService.insertTasks(tasks);

        assertEquals(1, inserted);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbcOperations).batchUpdate(anyString(), captor.capture());

        List<Object[]> batchArgs = captor.getValue();
        assertNull(batchArgs.get(0)[4]);
    }

    @Test
    void insertTasks_shouldThrowForInvalidDueDate() {
        List<TaskCreateRequest> tasks = List.of(
                task("Task A", "Desc A", "OPEN", "HIGH", "04-10-2026")
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.insertTasks(tasks)
        );

        assertEquals("Invalid dueDate format. Expected YYYY-MM-DD.", ex.getMessage());
        verify(jdbcOperations, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void insertTasks_shouldReturnOnlyPositiveInsertCounts() {
        List<TaskCreateRequest> tasks = List.of(
                task("Task A", "Desc A", "OPEN", "HIGH", "2026-04-10"),
                task("Task B", "Desc B", "DONE", "LOW", "2026-04-11"),
                task("Task C", "Desc C", "BLOCKED", "MEDIUM", "2026-04-12")
        );

        when(jdbcOperations.batchUpdate(anyString(), anyList()))
                .thenReturn(new int[]{1, 0, 1});

        int inserted = taskService.insertTasks(tasks);

        assertEquals(2, inserted);
    }

    @Test
    void insertTasks_shouldPropagateDatabaseFailure() {
        List<TaskCreateRequest> tasks = List.of(
                task("Task A", "Desc A", "OPEN", "HIGH", "2026-04-10")
        );

        when(jdbcOperations.batchUpdate(anyString(), anyList()))
                .thenThrow(new DataAccessResourceFailureException("DB down"));

        assertThrows(DataAccessResourceFailureException.class, () -> taskService.insertTasks(tasks));
    }

    @Test
    void getTotalCount_shouldReturnZeroWhenJdbcReturnsNull() {
        when(jdbcOperations.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        long result = taskService.getTotalCount();

        assertEquals(0, result);
    }

    @Test
    void getTotalCount_shouldReturnActualCount() {
        when(jdbcOperations.queryForObject(anyString(), eq(Long.class))).thenReturn(5L);

        long result = taskService.getTotalCount();

        assertEquals(5L, result);
    }

    @Test
    void getCountByStatus_shouldReturnOrderedMapFromRows() {
        when(jdbcOperations.queryForList(anyString())).thenReturn(List.of(
                Map.of("status", "DONE", "count", 2),
                Map.of("status", "OPEN", "count", 3)
        ));

        Map<String, Long> result = taskService.getCountByStatus();

        assertEquals(2, result.size());
        assertEquals(2L, result.get("DONE"));
        assertEquals(3L, result.get("OPEN"));
    }

    @Test
    void getCountByStatus_shouldReturnEmptyMapWhenNoRows() {
        when(jdbcOperations.queryForList(anyString())).thenReturn(List.of());

        Map<String, Long> result = taskService.getCountByStatus();

        assertTrue(result.isEmpty());
    }
}