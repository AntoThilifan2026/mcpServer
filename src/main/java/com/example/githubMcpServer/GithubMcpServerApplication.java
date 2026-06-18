package com.example.githubMcpServer;

import com.example.githubMcpServer.mcp.RepoTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GithubMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GithubMcpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider tools(RepoTools tools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(tools)
				.build();
	}

}
