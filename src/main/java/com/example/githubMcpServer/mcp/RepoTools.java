//package com.example.githubMcpServer.mcp;
//
//
//import com.example.githubMcpServer.service.GeminiService;
//import com.example.githubMcpServer.service.GitHubService;
//import com.example.githubMcpServer.service.PullRequestService;
//import com.example.githubMcpServer.util.SessionContext;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.nio.file.Files;
//
//@Component
//public class RepoTools {
//
//    private final GitHubService git;
//    private final GeminiService gemini;
//    private final PullRequestService pr;
//
//    private final SessionContext ctx = new SessionContext(); // ✅ STATE
//
//    public RepoTools(GitHubService git,
//                     GeminiService gemini,
//                     PullRequestService pr) {
//        this.git = git;
//        this.gemini = gemini;
//        this.pr = pr;
//    }
//
//    // ✅ 1. CLONE
//    @Tool(description = "Clone a GitHub repository")
//    public String cloneRepo(
//            @ToolParam(description = "Repo URL") String repoUrl) throws Exception {
//
//        //probably want to use temp directories...
//        //Path tempFile = Files.createTempFile(tempDir, "data-", ".txt");
//        //then keep it in a list and delete them when the connection ends
//        ctx.repoDir = new File("repo-" + System.currentTimeMillis());
//
//        git.cloneRepo(repoUrl, ctx.repoDir);
//
//        return "Repository cloned to " + ctx.repoDir.getAbsolutePath();
//    }
//
//    // ✅ 2. FIND POM or POMs
//    @Tool(description = "Find  root pom.xml in cloned repository")
//
//    public String findPom() throws Exception {
//
//        ctx.pomPath = Files.walk(ctx.repoDir.toPath())
//                .filter(p -> p.getFileName().toString().equals("pom.xml"))
//                .findFirst()
//                .orElseThrow();
//
//        return "Found pom.xml at " + ctx.pomPath;
//    }
//
//    // ✅ 3. READ POM this is a tool Probably need to have another one that lists all the poms
//    //then need list all child poms and give me the child pomn contents
//    // probably want to throw exception if child pom has a modules
//
//    //probably three tools. findrootpom, find childpoms (needs root pom filename as argument), read pom given filename as argument,
//
//    @Tool(description = "Read pom.xml content")
//    public String readPom() throws Exception {
//
//        ctx.originalContent = Files.readString(ctx.pomPath);
//
//        return ctx.originalContent.substring(0,
//                Math.min(500, ctx.originalContent.length()));
//    }
//
//    // ✅ 4. ANALYZE  <-- this is really bad. Agent
//    @Tool(description = "Analyze vulnerabilities in pom.xml")
//    public String analyzePom() {
//
//        return gemini.analyzeAndFix(ctx.originalContent);
//    }
//
//    // ✅ 5. This is bad too. We need to work out how to fix it. The LLM will want to load a file and save a file
//    // Probably it's the POM. So we need a way to support that...
//    //Think about how to do this
//    // options: rewrite whole pom (so this is really 'save POM')
//    //          given a pom file name (more than one pom in the system), and a group and a artifact id and a version, update the version
//    //error handling is important
//    @Tool(description = "Fix vulnerabilities in pom.xml")
//    public String fixPom() throws Exception {
//
//        ctx.fixedContent = gemini.analyzeAndFix(ctx.originalContent);
//
//        Files.write(ctx.pomPath, ctx.fixedContent.getBytes());
//
//        return "pom.xml updated with fixes";
//    }
//
//    // ✅ 6. CREATE BRANCH - tool good
//    @Tool(description = "Create a new Git branch")
//    public String createBranch() throws Exception {
//
//        ctx.branchName = "fix/cve-" + System.currentTimeMillis();
//
//        git.createBranch(ctx.repoDir, ctx.branchName);
//
//        return "Branch created: " + ctx.branchName;
//    }
//
//    // ✅ 7. PUSH //good tool.
//    @Tool(description = "Commit and push changes to GitHub")
//    public String pushChanges() throws Exception {
//
//        git.commitAndPush(ctx.repoDir, ctx.branchName, "AI Fix");
//
//        return "Changes pushed";
//    }
//
//    // ✅ 8. CREATE PR // good tool
//    @Tool(description = "Create pull request for changes")
//    public String createPR(
//            @ToolParam(description = "Repo owner") String owner,
//            @ToolParam(description = "Repo name") String repo) {
//
//        return pr.createPR(owner, repo, ctx.branchName);
//    }
//}