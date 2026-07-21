package com.example.githubMcpServer.util;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotSame;

class SessionIsolationRequirementTest {

    @Test
    void independentLookupsDoNotShareConversationOrSessionState() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.register(ConversationMemory.class, SessionContext.class);
            context.refresh();

            ConversationMemory firstMemory = context.getBean(ConversationMemory.class);
            ConversationMemory secondMemory = context.getBean(ConversationMemory.class);
            SessionContext firstSession = context.getBean(SessionContext.class);
            SessionContext secondSession = context.getBean(SessionContext.class);

            assertNotSame(firstMemory, secondMemory,
                    "Independent sessions must not share conversation memory");
            assertNotSame(firstSession, secondSession,
                    "Independent sessions must not share repository state");
        }
    }
}
