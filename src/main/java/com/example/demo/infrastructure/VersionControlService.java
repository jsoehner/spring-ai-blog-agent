package com.example.demo.infrastructure;

import java.util.List;

public interface VersionControlService {
    void createBranch(String branchName) throws Exception;
    void commit(String message) throws Exception;
    void push(String branchName) throws Exception;
    void createPullRequest(String title, String body) throws Exception;
    void addFiles(List<String> filePaths) throws Exception;
}
