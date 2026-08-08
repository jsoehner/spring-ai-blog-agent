package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ToolRegistry {
    private final Map<String, ExternalTool> tools = new HashMap<>();

    public void registerTool(ExternalTool tool) {
        tools.put(tool.getName(), tool);
    }

    public ExternalTool getTool(String name) {
        return tools.get(name);
    }

    public Map<String, ExternalTool> getAllTools() {
        return tools;
    }
}
