package org.acn.mcptaskservice.dto;



import java.util.Map;

public class TaskSummaryResponse {

    private String specVersion = "2025-06-18";
    private String tool = "mcp-tasks-summary";
    private long total;
    private Map<String, Long> byStatus;

    public TaskSummaryResponse(long total, Map<String, Long> byStatus) {
        this.total = total;
        this.byStatus = byStatus;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getTool() {
        return tool;
    }

    public long getTotal() {
        return total;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }
}