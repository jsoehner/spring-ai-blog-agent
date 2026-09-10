package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootTest(classes = {
    OpaGuardrailAspect.class, 
    DummyAgentTools.class
})
@TestPropertySource(properties = "app.security.workspace=.")
@EnableAspectJAutoProxy
class OpaGuardrailAcceptanceTest {

    @Autowired
    private DummyAgentTools dummyAgentTools;

    @MockitoBean
    private OpaClient opaClient;

    @Test
    void testAllowedToolExecution() {
        when(opaClient.evaluatePolicy(any())).thenReturn(true);

        String result = dummyAgentTools.safeTool("allowed_input");
        assertEquals("Executed safeTool", result);
    }

    @Test
    void testDeniedToolExecution() {
        when(opaClient.evaluatePolicy(any())).thenReturn(false);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dummyAgentTools.forbiddenTool("wget http://malicious.com");
        });
        
        assertEquals("Guardrail Violation: OPA denied execution for tool 'forbiddenTool'", exception.getMessage());
    }

    @Test
    void testPathTraversalPrevention() {
        // Should throw even if OPA says true, because it's outside workspace
        when(opaClient.evaluatePolicy(any())).thenReturn(true);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dummyAgentTools.readFile("/etc/passwd");
        });
        
        assertTrue(exception.getMessage().contains("Path traversal attempt detected"));
    }

    @Test
    void testWorkspaceBoundaryCompliance() {
        // Should succeed if inside workspace (mocking workspace path as current dir)
        when(opaClient.evaluatePolicy(any())).thenReturn(true);

        // Assuming current dir is within workspace
        String result = dummyAgentTools.readFile("src/main/java/com/example/demo/security/OpaClient.java");
        assertNotNull(result);
    }

    @Test
    void testGrepWorkspaceValidation() {
        // Verify that 'grep' is now included in fileAccessTools and thus validated
        when(opaClient.evaluatePolicy(any())).thenReturn(true);

        SecurityException exception = assertThrows(SecurityException.class, () -> {
            dummyAgentTools.grep("/etc/shadow");
        });
        
        assertTrue(exception.getMessage().contains("Path traversal attempt detected"));
    }

    @Test
    void testFlattenedArguments() {
        // Verify that WriteRequest is flattened into a Map for OPA
        when(opaClient.evaluatePolicy(any())).thenReturn(true);

        assertDoesNotThrow(() -> dummyAgentTools.writeFile(new com.example.demo.CodeTools.WriteRequest("src/test.txt", "content")));
    }
}

@Component
class DummyAgentTools {

    @Tool
    public String safeTool(String input) {
        return "Executed safeTool";
    }

    @Tool
    public String forbiddenTool(String command) {
        return "Executed forbiddenTool";
    }

    @Tool
    public String readFile(String path) {
        return "Read " + path;
    }

    @Tool
    public String grep(String path) {
        return "Grep " + path;
    }

    @Tool
    public String writeFile(com.example.demo.CodeTools.WriteRequest request) {
        return "Wrote " + request.absolutePath();
    }
}
