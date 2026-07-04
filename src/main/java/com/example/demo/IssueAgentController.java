package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Profile({"supervisor", "issue-agent"})
public class IssueAgentController {

    private final ChatClient chatClient;
    private final IssueAgentTools issueAgentTools;

    public IssueAgentController(ChatClient.Builder chatClientBuilder,
                                IssueAgentTools issueAgentTools,
                                @Value("${ISSUE_AGENT_PROMPT:You are an expert autonomous software engineer agent. Your goal is to resolve the given GitHub issue by inspecting the repository code, applying the required fixes or file additions, and submitting a Pull Request. Proceed step-by-step: First, call getGitHubIssue to get the details of the issue. Second, find the files needing modification using findFiles, listDirectory, or grepSearch. Third, read those files with readFile. Fourth, modify or create files using writeFile. Finally, submit the pull request with submitPullRequest. Always commit and submit your Pull Request to complete the task.}") String issueAgentPrompt) {
        this.issueAgentTools = issueAgentTools;
        
        // Follow Spring AI mutability guidelines: mutate the builder after building a base client
        this.chatClient = chatClientBuilder.build().mutate()
                .defaultSystem(issueAgentPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(issueAgentTools)
                .build();
    }

    @GetMapping("/issue/create-pr")
    public ResponseEntity<Map<String, String>> createPrGet(
            @RequestParam String issueRef,
            @RequestParam(required = false) String repository,
            @RequestParam(defaultValue = "main") String baseBranch) {
        return handleCreatePr(issueRef, repository, baseBranch);
    }

    @PostMapping("/issue/create-pr")
    public ResponseEntity<Map<String, String>> createPrPost(
            @RequestParam String issueRef,
            @RequestParam(required = false) String repository,
            @RequestParam(defaultValue = "main") String baseBranch) {
        return handleCreatePr(issueRef, repository, baseBranch);
    }

    private ResponseEntity<Map<String, String>> handleCreatePr(String issueRef, String repository, String baseBranch) {
        System.out.println("Issue Agent triggered for issue: " + issueRef + ", repo parameter: " + repository + ", base branch: " + baseBranch);
        
        Map<String, String> response = new HashMap<>();
        try {
            String resolvedIssueRef = issueRef;
            String owner = null;
            String repo = null;
            String issueNum = null;

            if (issueRef.contains("/")) {
                String[] parts = issueRef.split("/");
                if (parts.length >= 4 && "issues".equals(parts[2])) {
                    owner = parts[0];
                    repo = parts[1];
                    issueNum = parts[3];
                } else if (parts.length == 3) {
                    owner = parts[0];
                    repo = parts[1];
                    issueNum = parts[2];
                    resolvedIssueRef = owner + "/" + repo + "/issues/" + issueNum;
                }
            } else if (repository != null && repository.contains("/")) {
                String[] parts = repository.split("/");
                owner = parts[0];
                repo = parts[1];
                issueNum = issueRef;
                resolvedIssueRef = owner + "/" + repo + "/issues/" + issueNum;
            }

            if (owner == null || repo == null) {
                try {
                    String remoteUrl = issueAgentTools.getLocalGitRemote();
                    if (remoteUrl.contains("github.com")) {
                        String ownerRepo = "";
                        if (remoteUrl.startsWith("https://")) {
                            ownerRepo = remoteUrl.substring(remoteUrl.indexOf("github.com/") + 11);
                        } else if (remoteUrl.startsWith("git@github.com:")) {
                            ownerRepo = remoteUrl.substring(remoteUrl.indexOf("git@github.com:") + 15);
                        }
                        if (ownerRepo.endsWith(".git")) {
                            ownerRepo = ownerRepo.substring(0, ownerRepo.length() - 4);
                        }
                        String[] parts = ownerRepo.split("/");
                        owner = parts[0];
                        repo = parts[1];
                    }
                } catch (Exception e) {
                    owner = "jsoehner";
                    repo = "spring-ai-blog-agent";
                }
                issueNum = issueRef;
                resolvedIssueRef = owner + "/" + repo + "/issues/" + issueNum;
            }

            System.out.println("Setting up repository: " + owner + "/" + repo + " on base branch: " + baseBranch);
            java.nio.file.Path repoPath = issueAgentTools.setupRepository(owner, repo, baseBranch);
            
            // Set current repo root in ThreadLocal
            issueAgentTools.setCurrentRepoRoot(repoPath);
            try {
                // Trigger the AI ChatClient prompt
                String promptUserMessage = String.format(
                    "Please resolve the GitHub issue: '%s'. The base branch is '%s'.", 
                    resolvedIssueRef, baseBranch
                );

                String result = chatClient.prompt()
                        .user(promptUserMessage)
                        .call()
                        .content();

                response.put("status", "success");
                response.put("message", result);
                return ResponseEntity.ok(response);
            } finally {
                issueAgentTools.clearCurrentRepoRoot();
            }

        } catch (Exception e) {
            System.err.println("Error processing issue " + issueRef + ": " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
