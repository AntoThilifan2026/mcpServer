    package com.example.githubMcpServer.mcp;

    import com.example.githubMcpServer.service.GitHubService;
    import com.example.githubMcpServer.service.PullRequestService;

    import org.springframework.ai.tool.annotation.Tool;
    import org.springframework.ai.tool.annotation.ToolParam;
    import org.springframework.stereotype.Component;

    import java.io.File;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.Comparator;
    import java.util.List;

    @Component
    public class McpTools {

            private final GitHubService git;
            private final PullRequestService pr;

            public McpTools(GitHubService git,
                             PullRequestService pr) {
                this.git = git;
                this.pr = pr;
            }

            @Tool(description = "Clone a GitHub repository")
            public String cloneRepo(
                    @ToolParam(description = "Repository URL")
                    String repoUrl) throws Exception {
                Path repoDir = Files.createTempDirectory("repoTemporary-");
                git.cloneRepo(repoUrl, repoDir.toFile());
                return repoDir.toAbsolutePath().toString();
            }

            @Tool(description = "Find root pom.xml")
            public String findRootPom(
                    @ToolParam(description = "Repository directory")
                    String repoDir) throws Exception {

                Path rootPom = Files.walk(Path.of(repoDir))
                        .filter(path -> path.getFileName().toString().equals("pom.xml"))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException("No root pom.xml found"));

                return rootPom.toString();
            }

        @Tool(description = "Find child pom.xml files")
        public List<String> findChildPoms(
                @ToolParam(description = "Repository directory")
                String repoDir) throws Exception {

            Path rootPom = Path.of(repoDir, "pom.xml");

            return Files.walk(Path.of(repoDir))
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .equals("pom.xml"))
                    .filter(path ->
                            !path.toAbsolutePath()
                                    .equals(rootPom.toAbsolutePath()))
                    .map(path ->
                            path.toAbsolutePath().toString())
                    .toList();
        }

        @Tool(description = "Read pom.xml content by file path")
        public String readPom(
                @ToolParam(description = "Pom file path")
                String pomPath) throws Exception {

            Path path = Path.of(pomPath);

            if (!Files.exists(path)) {
                throw new RuntimeException(
                        "Pom file not found: " + pomPath);
            }

            return Files.readString(path);
        }

            @Tool(description = "Save pom content")
            public String savePom(
                    @ToolParam(description = "Pom path")
                    String pomPath,

                    @ToolParam(description = "Updated pom content")
                    String content) throws Exception {

                Files.writeString(
                        Path.of(pomPath),
                        content
                );

                return "POM updated successfully";
            }

            @Tool(description = "Create git branch")
            public String createBranch(

                    @ToolParam(description = "Repository directory")
                    String repoDir,

                    @ToolParam(description = "Branch name")
                    String branchName

            ) throws Exception {

                git.createBranch(
                        new File(repoDir),
                        branchName
                );

                return branchName;
            }

            @Tool(description = "Commit and push changes")
            public String pushChanges(

                    @ToolParam(description = "Repository directory")
                    String repoDir,

                    @ToolParam(description = "Branch name")
                    String branchName,

                    @ToolParam(description = "Commit message")
                    String commitMessage

            ) throws Exception {git.commitAndPush(new File(repoDir), branchName, commitMessage);

                return "Changes pushed";
            }

            @Tool(description = "Create GitHub pull request")
            public String createPR(
                    @ToolParam(description = "Repository owner")
                    String owner,
                    @ToolParam(description = "Repository name")
                    String repo,
                    @ToolParam(description = "Branch name")
                    String branchName
            ) {
                return pr.createPR(owner, repo, branchName);
            }

            @Tool(description = "Delete temporary repository")
            public String cleanupRepository(

                    @ToolParam(description = "Repository directory")
                    String repoDir

            ) throws Exception {

                Files.walk(Path.of(repoDir))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                return "Repository deleted";
            }

        }

