package org.acn.mcptaskservice.controller;

import org.acn.mcptaskservice.mcp.McpProtocolException;
import org.acn.mcptaskservice.mcp.McpProtocolService;
import org.acn.mcptaskservice.mcp.McpSession;
import org.acn.mcptaskservice.mcp.McpSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/mcp")
public class McpHttpController {

    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS = Set.of(
            McpProtocolService.SPEC_VERSION,
            "2025-03-26");

    private final McpProtocolService mcpProtocolService;
    private final McpSessionService sessionService;
    private final Set<String> allowedOrigins;

    public McpHttpController(
            McpProtocolService mcpProtocolService,
            McpSessionService sessionService,
            @Value("${mcp.allowed-origins:http://localhost,http://127.0.0.1}") String allowedOrigins) {
        this.mcpProtocolService = mcpProtocolService;
        this.sessionService = sessionService;
        this.allowedOrigins = Stream.of(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postMessage(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocolHeader,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestBody Map<String, Object> body) {

        Object id = body.get("id");
        try {
            validateOrigin(origin);
            validatePostAccept(accept);

            Object result = body.get("result");
            Object error = body.get("error");
            String method = body.get("method") instanceof String s ? s : null;
            String jsonrpc = body.get("jsonrpc") instanceof String s ? s : null;

            if (!"2.0".equals(jsonrpc)) {
                return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32600, "jsonrpc must be '2.0'");
            }

            if (method == null || method.isBlank()) {
                if (id != null && (result != null || error != null)) {
                    return ResponseEntity.accepted().build();
                }
                return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32600,
                        "method is required for requests and notifications");
            }

            boolean isNotification = id == null;

            if ("initialize".equals(method)) {
                if (sessionId != null && !sessionId.isBlank()) {
                    return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32600,
                            "initialize must not include Mcp-Session-Id");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> params = body.get("params") instanceof Map<?, ?> p ? (Map<String, Object>) p
                        : Map.of();
                String requestedVersion = params.get("protocolVersion") instanceof String s ? s : null;
                if (requestedVersion == null || !SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion)) {
                    return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32602, "Unsupported protocol version");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> clientInfo = params.get("clientInfo") instanceof Map<?, ?> c
                        ? (Map<String, Object>) c
                        : Map.of();
                String clientName = clientInfo.get("name") instanceof String s ? s : "unknown-client";
                String clientVersion = clientInfo.get("version") instanceof String s ? s : "unknown";

                McpSession session = sessionService.createSession(McpProtocolService.SPEC_VERSION, clientName,
                        clientVersion);
                Map<String, Object> initResult = mcpProtocolService.initialize(params);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", session.getId())
                        .body(jsonRpcResult(id, initResult));
            }

            McpSession session = requireSession(sessionId);
            String negotiatedProtocol = resolveProtocolVersion(protocolHeader, session);
            if (!session.getProtocolVersion().equals(negotiatedProtocol)) {
                return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32600,
                        "Protocol version does not match initialized session");
            }

            if (isNotification) {
                handleNotification(method, session);
                return ResponseEntity.accepted()
                        .header("MCP-Protocol-Version", session.getProtocolVersion())
                        .build();
            }

            if (!session.isInitializedNotificationReceived()) {
                return jsonRpcError(HttpStatus.BAD_REQUEST, id, -32002,
                        "Client must send notifications/initialized before normal operations");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = body.get("params") instanceof Map<?, ?> p ? (Map<String, Object>) p : Map.of();
            Map<String, Object> rpcResult = switch (method) {
                case "ping" -> Map.of();
                case "tools/list" -> mcpProtocolService.listTools();
                case "tools/call" -> mcpProtocolService.callTool(params);
                default -> throw new McpProtocolException(-32601, "Method not found: " + method);
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("MCP-Protocol-Version", session.getProtocolVersion())
                    .body(jsonRpcResult(id, rpcResult));
        } catch (UnknownSessionException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (McpProtocolException ex) {
            return jsonRpcError(HttpStatus.BAD_REQUEST, id, ex.getCode(), ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Void> openSse(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId) {
        validateOrigin(origin);
        validateGetAccept(accept);

        if (sessionId != null && !sessionId.isBlank() && sessionService.findSession(sessionId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, "POST, DELETE")
                .build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteSession(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId) {
        validateOrigin(origin);
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean removed = sessionService.deleteSession(sessionId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private McpSession requireSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new McpProtocolException(-32600, "Missing Mcp-Session-Id header");
        }
        return sessionService.findSession(sessionId)
                .orElseThrow(UnknownSessionException::new);
    }

    private String resolveProtocolVersion(String protocolHeader, McpSession session) {
        if (protocolHeader == null || protocolHeader.isBlank()) {
            return session.getProtocolVersion();
        }
        if (!SUPPORTED_PROTOCOL_VERSIONS.contains(protocolHeader)) {
            throw new McpProtocolException(-32600, "Invalid or unsupported MCP-Protocol-Version header");
        }
        return protocolHeader;
    }

    private void handleNotification(String method, McpSession session) {
        if ("notifications/initialized".equals(method)) {
            session.markInitializedNotificationReceived();
            return;
        }
        if ("notifications/cancelled".equals(method)) {
            return;
        }
        throw new McpProtocolException(-32601, "Unsupported notification: " + method);
    }

    private void validateOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return;
        }
        URI uri = URI.create(origin);
        String normalized = uri.getScheme() + "://" + uri.getHost();
        if (!allowedOrigins.contains(normalized)) {
            throw new McpProtocolException(-32600, "Origin not allowed");
        }
    }

    private void validatePostAccept(String accept) {
        if (accept == null || accept.isBlank()) {
            return;
        }
        String normalized = accept.toLowerCase();
        boolean supportsJson = normalized.contains("application/json") || normalized.contains("*/*");
        boolean supportsSse = normalized.contains("text/event-stream") || normalized.contains("*/*");
        if (!supportsJson || !supportsSse) {
            throw new McpProtocolException(-32600, "Accept header must include application/json and text/event-stream");
        }
    }

    private void validateGetAccept(String accept) {
        if (accept == null || accept.isBlank()) {
            return;
        }
        String normalized = accept.toLowerCase();
        if (!normalized.contains("text/event-stream") && !normalized.contains("*/*")) {
            throw new McpProtocolException(-32600, "Accept header must include text/event-stream");
        }
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(HttpStatus status, Object id, int code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("error", Map.of(
                "code", code,
                "message", message));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
    }

    private Map<String, Object> jsonRpcResult(Object id, Map<String, Object> result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("result", result);
        return payload;
    }

    private static final class UnknownSessionException extends RuntimeException {
    }
}
