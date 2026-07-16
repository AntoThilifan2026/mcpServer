package com.example.githubMcpServer.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConversationMemory {

    private final List<String> messages = new ArrayList<>();

    public void add(String role, String message) {

        messages.add(
                role + ": " + message);
    }

    public String getConversation() {

        return String.join("\n", messages);
    }
}