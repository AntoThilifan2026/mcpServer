package com.example.githubMcpServer.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoveryServiceTest {

    @Test
    void unavailableMcpServerIsReportedToTheCaller() {
        McpToolDiscoveryService service = new McpToolDiscoveryService();
        ReflectionTestUtils.setField(service, "mcpDiscoveryUrl", "not-a-valid-url");

        assertThrows(
                RuntimeException.class,
                service::discoverTools,
                "MCP discovery failures must not be silently converted into an empty tool list");
    }

    @Test
    void mcpDiscoveryHasMeaningfulConnectAndReadTimeouts() {
        McpToolDiscoveryService service = new McpToolDiscoveryService();
        RestTemplate restTemplate =
                (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        int connectTimeout =
                (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
        int readTimeout =
                (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");

        assertTrue(connectTimeout > 0, "MCP connect timeout must be configured");
        assertTrue(readTimeout > 0, "MCP read timeout must be configured");
    }
}
