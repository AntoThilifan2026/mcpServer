package com.example.githubMcpServer.util;

public record ToolCall(
        String id,
        String name,
        String arguments)
        implements Message {
}