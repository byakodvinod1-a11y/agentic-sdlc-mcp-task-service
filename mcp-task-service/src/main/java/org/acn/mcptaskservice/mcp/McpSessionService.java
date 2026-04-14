package org.acn.mcptaskservice.mcp;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpSessionService {

    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

    public McpSession createSession(String protocolVersion, String clientName, String clientVersion) {
        String sessionId = UUID.randomUUID().toString();
        McpSession session = new McpSession(sessionId, protocolVersion, clientName, clientVersion);
        sessions.put(sessionId, session);
        return session;
    }

    public Optional<McpSession> findSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public boolean deleteSession(String sessionId) {
        return sessions.remove(sessionId) != null;
    }
}
