package com.example.githubMcpServer.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class ConversationMemory {

    private final List<Message> messages =
            new ArrayList<>();

    public void add(Message message) {

        messages.add(message);
    }

    public List<Message> getMessages() {

        return new ArrayList<>(messages);
    }

    public void clear() {

        messages.clear();
    }
}