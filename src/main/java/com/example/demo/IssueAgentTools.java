package com.example.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class IssueAgentTools {

    private final RestTemplate restTemplate;

    public IssueAgentTools(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record PullRequestRequest(
        String branchName,
        String commitMessage,
        String prTitle,
        String prBody
    ) {}

    @Tool(description = "Fetches the GitHub issue details (title, description/body, state, url) for the given issue reference (format: 'owner/repo/issues/number').")
    public String getGitHubIssue(String issueRef) {
        try {
            // Parse reference (e.g. clover0/example-repository/issues/123)
            String[] parts = issueRef.split("/");
            if (parts.length < 4 || !"issues".equals(parts[2])) {
                return "Error: Invalid issue reference format. Expected 'owner/repo/issues/number'. Got: " + issueRef;
            }
            String owner = parts[0];
            String repo = parts[1];
            String number = parts[3];

            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + number;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github+json");
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            headers.set("User-Agent", "Spring-AI-Issue-Agent");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String title = (String) body.get("title");
                String issueBody = (String) body.get("body");
                String state = (String) body.get("state");
                String htmlUrl = (String) body.get("html_url");
                
                return String.format("Title: %s\n\nState: %s\n\nURL: %s\n\nDescription:\n%s", title, state, htmlUrl, issueBody);
            } else {
                return "Error fetching issue: " + response.getStatusCode();
            }
        } catch (Exception e) {
            return "Error fetching issue: " + e.getMessage();
        }
    }

    @Tool(description = "Lists files and subdirectories in the specified path (relative to the repository root). Use empty string '' or '.' to list the root directory.")
    public String listDirectory(String relativePath) {
        try {
            Path root = Paths.get(".").toAbsolutePath().normalize();
            Path target = root.resolve(relativePath == null ? "" : relativePath).toAbsolutePath().normalize();
            
            if (!target.startsWith(root)) {
                return "Error: Access denied (path traversal prevented).";
            }
            
            if (!Files.exists(target)) {
                return "Error: Path does not exist: " + relativePath;
            }
            
            StringBuilder sb = new StringBuilder();
            try (var stream = Files.list(target)) {
                stream.forEach(p -> {
                    String name = p.getFileName().toString();
                    if (Files.isDirectory(p)) {
                        sb.append("[DIR] ").append(name).append("\n");
                    } else {
                        sb.append("[FILE] ").append(name).append(" (").append(p.toFile().length()).append(" bytes)\n");
                    }
                });
            }
            return sb.toString().isEmpty() ? "(Directory is empty)" : sb.toString();
        } catch (Exception e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    @Tool(description = "Finds files recursively under the repository matching a specific name pattern (e.g., '*.java', 'pom.xml').")
    public String findFiles(String pattern) {
        try {
            Path root = Paths.get(".").toAbsolutePath().normalize();
            List<String> matches = new ArrayList<>();
            
            String globPattern = pattern.toLowerCase()
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
            
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String relative = root.relativize(p).toString();
                        if (relative.startsWith(".git") || relative.startsWith(".gradle") || relative.startsWith("build/") || relative.startsWith(".venv")) {
                            return;
                        }
                        if (p.getFileName().toString().toLowerCase().matches(globPattern)) {
                            matches.add(relative);
                        }
                    });
            }
                
            return matches.isEmpty() ? "No matching files found." : String.join("\n", matches);
        } catch (Exception e) {
            return "Error finding files: " + e.getMessage();
        }
    }

    @Tool(description = "Reads the contents of a text/source file from the local repository. Input is the relative path (e.g. 'src/main/java/App.java').")
    public String readFile(String relativePath) {
        try {
            Path root = Paths.get(".").toAbsolutePath().normalize();
            Path target = root.resolve(relativePath).toAbsolutePath().normalize();
            
            if (!target.startsWith(root)) {
                return "Error: Access denied (path traversal prevented).";
            }
            
            if (!Files.exists(target)) {
                return "Error: File does not exist: " + relativePath;
            }
            
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Writes or overwrites a file with the provided content. Use this to save code changes, apply bug fixes, or create new files. Input requires the relative file path and the complete new content of the file.")
    public String writeFile(String relativePath, String content) {
        try {
            Path root = Paths.get(".").toAbsolutePath().normalize();
            Path target = root.resolve(relativePath).toAbsolutePath().normalize();
            
            if (!target.startsWith(root)) {
                return "Error: Access denied (path traversal prevented).";
            }
            
            // Create parent directories if they don't exist
            if (target.getParent() != null && !Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
            
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return "Successfully wrote file to " + relativePath;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool(description = "Searches for a text pattern or term in all files in the repository (excluding build and version control directories).")
    public String grepSearch(String term) {
        try {
            Path root = Paths.get(".").toAbsolutePath().normalize();
            StringBuilder sb = new StringBuilder();
            int matchCount = 0;
            
            try (var stream = Files.walk(root)) {
                var fileList = stream.filter(Files::isRegularFile).toList();
                for (Path p : fileList) {
                    String relative = root.relativize(p).toString();
                    if (relative.startsWith(".git") || relative.startsWith(".gradle") || relative.startsWith("build/") || relative.startsWith(".venv") || relative.startsWith("request-activity.log") || relative.endsWith(".png") || relative.endsWith(".jar")) {
                        continue;
                    }
                    
                    try {
                        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.toLowerCase().contains(term.toLowerCase())) {
                                sb.append(relative).append(":").append(i + 1).append(": ").append(line.trim()).append("\n");
                                matchCount++;
                                if (matchCount >= 100) {
                                    sb.append("... Truncated (too many matches) ...");
                                    return sb.toString();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore binary or decoding errors
                    }
                }
            }
            return sb.toString().isEmpty() ? "No matches found." : sb.toString();
        } catch (Exception e) {
            return "Error performing grep search: " + e.getMessage();
        }
    }

    @Tool(description = "Commits all modified and new files, pushes the branch to GitHub, and creates a Pull Request. Ensure code changes are saved using write_file before calling this.")
    public String submitPullRequest(PullRequestRequest request) {
        try {
            String branchName = request.branchName().replaceAll("[^a-zA-Z0-9-]", "-");
            String commitMessage = request.commitMessage().replace("'", "\"");
            String prTitle = request.prTitle().replace("'", "\"");
            String prBody = request.prBody().replace("'", "\"");

            // Verify git status first
            String status = runSystemCommand("git status --short");
            if (status.trim().isEmpty()) {
                return "Error: No changes to commit.";
            }

            // 1. Git config (only if not set)
            runSystemCommand("git config user.email 'agent@spring-ai.local'");
            runSystemCommand("git config user.name 'Spring AI Agent'");

            // 2. Checkout branch
            runSystemCommand("git checkout -B " + branchName);

            // 3. Add files
            runSystemCommand("git add -A");

            // 4. Commit
            runSystemCommand("git commit -m '" + commitMessage + "'");

            // 5. If GITHUB_TOKEN is available, configure remote origin to authenticate
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.isEmpty()) {
                String remoteUrl = runSystemCommand("git remote get-url origin").trim();
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
                    if (!ownerRepo.isEmpty()) {
                        String authenticatedUrl = "https://x-access-token:" + token + "@github.com/" + ownerRepo + ".git";
                        runSystemCommand("git remote set-url origin " + authenticatedUrl);
                    }
                }
            }

            // 6. Push
            String pushOutput = runSystemCommand("git push -u origin " + branchName);

            // 7. Create PR via GitHub CLI
            String prOutput = runSystemCommand("gh pr create --title '" + prTitle + "' --body '" + prBody + "'");

            return "Pull Request created successfully!\n\nPush Output:\n" + pushOutput + "\nPR Output:\n" + prOutput;

        } catch (Exception e) {
            return "Failed to submit Pull Request: " + e.getMessage();
        }
    }

    private String runSystemCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command '" + command + "' failed with exit code " + exitCode + ". Output: " + output);
        }
        return output.toString();
    }
}
