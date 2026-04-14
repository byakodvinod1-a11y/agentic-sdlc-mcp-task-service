package org.acn.mcptaskservice.mcp;

public class McpProtocolException extends RuntimeException {

    private final int code;

    public McpProtocolException(String message) {
        super(message);
        this.code = -32600; // default: invalid request
    }

    public McpProtocolException(String message, Throwable cause) {
        super(message, cause);
        this.code = -32600;
    }

    public McpProtocolException(int code, String message) {
        super(message);
        this.code = code;
    }

    public McpProtocolException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}