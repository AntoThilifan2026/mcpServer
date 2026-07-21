package com.example.githubMcpServer.service;

import com.example.githubMcpServer.util.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class McpToolDiscoveryService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mcp.discovery.url:http://localhost:8080/mcp}")
    private String mcpDiscoveryUrl;

    public List<McpTool> discoverTools() {
        List<McpTool> result = new ArrayList<>();

        try {
            Map<String, Object> request = Map.of(
                    "jsonrpc", "2.0",
                    "id", "tools-list-1",
                    "method", "tools/list",
                    "params", Map.of());

            Map<?, ?> response = restTemplate.postForObject(
                            mcpDiscoveryUrl,
                            request,
                            Map.class);

            if (response == null) {
                return result;
            }

            Object resultNode = response.get("result");
            List<Map<String, Object>> tools = null;

            if (resultNode instanceof Map<?, ?> resultMap) {
                Object toolsNode = resultMap.get("tools");
                if (toolsNode instanceof List<?> toolsList) {
                    tools = (List<Map<String, Object>>) toolsList;
                }
            }

            if (tools == null && response.get("tools") instanceof List<?> rootTools) {
                tools = (List<Map<String, Object>>) rootTools;
            }

            if (tools == null) {
                return result;
            }

            for (Map<String, Object> tool : tools) {
                result.add(
                        new McpTool(
                                (String) tool.get("name"),
                                (String) tool.get("description"),
                                (Map<String, Object>) tool.get("inputSchema")));
            }
        } catch (Exception ignored) {
            return result;
        }

        return result;
    }
}