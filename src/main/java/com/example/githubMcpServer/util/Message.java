package com.example.githubMcpServer.util;


public sealed interface Message permits
        TextMessage, ToolCall, ToolResult {
}