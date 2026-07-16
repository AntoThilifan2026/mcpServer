package com.example.githubMcpServer.controller;

import com.example.githubMcpServer.service.AgentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class AgentConsole
        implements CommandLineRunner {

    private final AgentService agentService;

    public AgentConsole(
            AgentService agentService) {

        this.agentService =
                agentService;
    }

    @Override
    public void run(String... args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.println("GitHub Agent Started");

        System.out.println("Type 'exit' to quit");

        while (true) {
            System.out.print("\nYou: ");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            try {
                String response = agentService.process(input);
                System.out.println("\nAgent: " + response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}