package org.acn.mcptaskservice.dto;


import java.util.List;

public class TaskInsertResponse {

    private String specVersion = "2025-06-18";
    private String tool = "mcp-tasks";
    private int requested;
    private int inserted;
    private int errors;
    private List<String> errorSamples;

    public TaskInsertResponse(int requested, int inserted, List<String> errorSamples) {
        this.requested = requested;
        this.inserted = inserted;
        this.errors = requested - inserted;
        this.errorSamples = errorSamples;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getTool() {
        return tool;
    }

    public int getRequested() {
        return requested;
    }

    public int getInserted() {
        return inserted;
    }

    public int getErrors() {
        return errors;
    }

    public List<String> getErrorSamples() {
        return errorSamples;
    }
}