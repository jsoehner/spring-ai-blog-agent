package com.example.demo.service;

import java.util.Map;

public interface ExternalTool {
    String getName();
    String getDescription();
    Object execute(Map<String, Object> inputs) throws Exception;
}
