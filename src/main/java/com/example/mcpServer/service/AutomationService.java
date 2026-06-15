package com.example.mcpServer.service;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class AutomationService {

    private final GitHubService gitService;
    private final GeminiService geminiService;
    private final PullRequestService prService;

    public AutomationService(GitHubService gitService,
                             GeminiService geminiService,
                             PullRequestService prService) {
        this.gitService = gitService;
        this.geminiService = geminiService;
        this.prService = prService;
    }
    public String runAutomation() {

        try {
            //  Step 0: Define repo directory
            File repoDir = new File("demo");

            //  Step 1: Delete if already exists
            deleteIfExists(repoDir);

            // Step 2: Clone repo
            repoDir = gitService.cloneRepo();

            //  Step 3: Find pom.xml recursively
            Path pomPath = findPomFile(repoDir.toPath());
            System.out.println("Found pom.xml at: " + pomPath);

            //  Step 4: Read pom.xml
            String content = Files.readString(pomPath);

            //  Step 5: Send to Gemini
            String fixedContent = geminiService.analyzeAndFix(content);

            //  Step 6: Create branch
            String branchName = "fix/cve-" + System.currentTimeMillis();
            gitService.createBranch(branchName);

            //  Step 7: Update file
            gitService.updateFile(pomPath.toString(), fixedContent);

            // Step 8: Commit & Push
            gitService.commitAndPush("Fix CVE via Gemini AI");

            //  Step 9: Create PR
            return prService.createPR("AntoThilifan2026", "mcp-demo", branchName);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    //  Utility method to find pom.xml
    private Path findPomFile(Path repoDir) throws Exception {

        try (Stream<Path> paths = Files.walk(repoDir)) {

            Optional<Path> pomPath = paths
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("pom.xml"))
                    .findFirst();

            if (pomPath.isPresent()) {
                return pomPath.get();
            } else {
                throw new RuntimeException("pom.xml not found in repo!");
            }
        }
    }

    private void deleteIfExists(File dir) throws Exception {
        if (dir.exists()) {
            System.out.println("Deleting existing folder: " + dir.getAbsolutePath());

            // small delay (prevents Windows file lock issue)
            Thread.sleep(1000);

            FileUtils.deleteDirectory(dir);

            System.out.println("Deleted successfully");
        }
    }
}