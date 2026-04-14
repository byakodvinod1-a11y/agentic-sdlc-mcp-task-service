package org.acn.mcptaskservice.mcp;

import java.time.Instant;

public class McpSession {

    private final String id;
    private final String protocolVersion;
    private final String clientName;
    private final String clientVersion;
    private final Instant createdAt;
    private volatile boolean initializedNotificationReceived;

    public McpSession(String id, String protocolVersion, String clientName, String clientVersion) {
        this.id = id;
        this.protocolVersion = protocolVersion;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isInitializedNotificationReceived() {
        return initializedNotificationReceived;
    }

    public void markInitializedNotificationReceived() {
        this.initializedNotificationReceived = true;
    }
}
