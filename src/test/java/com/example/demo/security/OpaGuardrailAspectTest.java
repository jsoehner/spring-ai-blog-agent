package com.example.demo.security;

import com.example.demo.CodeTools.WriteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    OpaGuardrailAspect.class,
    DummyAspectTools.class
})
@EnableAspectJAutoProxy
class OpaGuardrailAspectTest {

    @Autowired
    private DummyAspectTools dummyAspectTools;

    @MockitoBean
    private OpaClient opaClient;

    @Test
    void testAllowedTool() {
        when(opaClient.evaluatePolicy(any())).thenReturn(true);
        String result = dummyAspectTools.normalTool("hello");
        assertEquals("normal", result);
    }

    @Test
    void testDeniedTool() {
        when(opaClient.evaluatePolicy(any())).thenReturn(false);
        assertThrows(SecurityException.class, () -> {
            dummyAspectTools.normalTool("hello");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWriteFileToolPathExtraction() {
        when(opaClient.evaluatePolicy(argThat(argument -> {
            Map<String, Object> input = (Map<String, Object>) argument;
            if ("file".equals(input.get("resource_type"))) {
                Map<String, Object> request = (Map<String, Object>) input.get("request");
                return "/tmp/safe.txt".equals(request.get("path"));
            }
            return false;
        }))).thenReturn(true);

        WriteRequest safeRequest = new WriteRequest("/tmp/safe.txt", "content");
        String result = dummyAspectTools.writeFile(safeRequest);
        assertEquals("written", result);

        WriteRequest unsafeRequest = new WriteRequest("/tmp/unsafe.txt", "content");
        assertThrows(SecurityException.class, () -> {
            dummyAspectTools.writeFile(unsafeRequest);
        });
    }

    @Test
    void testOpaClientThrowsExceptionFailsSafe() {
        when(opaClient.evaluatePolicy(any())).thenThrow(new RuntimeException("OPA server down"));
        assertThrows(RuntimeException.class, () -> {
            dummyAspectTools.normalTool("hello");
        });
    }
}

@Component
class DummyAspectTools {

    @Tool
    public String normalTool(String arg) {
        return "normal";
    }

    @Tool
    public String writeFile(WriteRequest request) {
        return "written";
    }

    @Tool
    public String readFile(String path) {
        return "read";
    }
}
