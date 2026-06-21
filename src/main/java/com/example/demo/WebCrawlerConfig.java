package com.example.demo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Arrays;

@Component
public class WebCrawlerConfig {

    @Value("${webcrawler.default.urls:https://thehackernews.com/,https://www.bleepingcomputer.com/,https://www.schneier.com/,https://krebsonsecurity.com/,https://haveibeenpwned.com/,https://www.identitytheft.gov/,https://www.virustotal.com/,https://owasp.org/,https://www.cisa.gov/,https://staysafeonline.org/}")
    private List<String> defaultUrls;

    @Tool(description = "Returns a curated list of default websites for researching topics when web search fails.")
    public List<String> getDefaultSearchSites() {
        System.out.println("Researcher Agent retrieving default search sites list...");
        return defaultUrls;
    }

    @Tool(description = "Searches the web for a given topic and returns a list of relevant URLs.")
    public List<String> searchWeb(String query) {
        System.out.println("Researcher Agent searching Wikipedia for: " + query);
        List<String> urls = new java.util.ArrayList<>();
        try {
            String url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + 
                         java.net.URLEncoder.encode(query, "UTF-8") + "&utf8=&format=json";
            
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 Spring AI Agent")
                    .timeout(5000)
                    .get();
                    
            String json = doc.body().text();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(json);
            com.fasterxml.jackson.databind.JsonNode searchResults = rootNode.path("query").path("search");
            
            for (com.fasterxml.jackson.databind.JsonNode node : searchResults) {
                String title = node.path("title").asText().replace(" ", "_");
                urls.add("https://en.wikipedia.org/wiki/" + java.net.URLEncoder.encode(title, "UTF-8"));
            }
            
            if (urls.isEmpty()) {
                System.out.println("Search returned no URLs, falling back to default search sites.");
                return defaultUrls;
            }
        } catch (Exception e) {
            System.out.println("Researcher Agent failed to search: " + e.getMessage());
            return defaultUrls;
        }
        return urls;
    }

    @Tool(description = "Crawls a list of websites and extracts their text content. Use this to read the contents of URLs you decide are relevant.")
    public String crawl(List<String> urls) {
        System.out.println("Researcher Agent starting to crawl " + urls.size() + " URLs...");
        StringBuilder allContent = new StringBuilder();
        for (String url : urls) {
            try {
                String formattedUrl = url;
                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                    formattedUrl = "https://" + formattedUrl;
                }
                System.out.println("Researcher Agent crawling: " + formattedUrl);
                Document doc = Jsoup.connect(formattedUrl)
                        .userAgent("Mozilla/5.0 Spring AI Agent")
                        .timeout(5000)
                        .get();
                // Extract text from the page
                String text = doc.body().text();
                String title = doc.title();
                
                System.out.println("Researcher Agent extracted title: " + title);
                System.out.println("Researcher Agent text snippet: " + text.substring(0, Math.min(text.length(), 100)).replace('\n', ' ') + "...");
                
                // Limit the text to avoid context overflow for the LLM
                if (text.length() > 2000) {
                    text = text.substring(0, 2000) + "... (truncated)";
                }
                allContent.append("Content from ").append(url).append(":\n").append(text).append("\n\n");
            } catch (Exception e) {
                System.out.println("Researcher Agent failed to crawl: " + url + " - " + e.getMessage());
                allContent.append("Failed to crawl ").append(url).append(": ").append(e.getMessage()).append("\n\n");
            }
        }
        return allContent.toString();
    }
}
