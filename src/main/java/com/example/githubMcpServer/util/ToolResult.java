package com.example.githubMcpServer.util;

public record ToolResult(
        String toolCallId,
        String result)
        implements Message {
}