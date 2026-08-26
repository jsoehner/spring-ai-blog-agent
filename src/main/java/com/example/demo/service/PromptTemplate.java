package com.example.demo.service;

import java.util.Objects;

public class PromptTemplate {
    private String name;
    private String version;
    private String description;
    private String content;

    public PromptTemplate() {
    }

    public PromptTemplate(String name, String version, String description, String content) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromptTemplate that = (PromptTemplate) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(description, that.description) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, description, content);
    }

    @Override
    public String toString() {
        return "PromptTemplate{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
