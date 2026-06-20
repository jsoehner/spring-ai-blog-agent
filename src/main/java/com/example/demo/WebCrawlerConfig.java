package com.example.demo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Arrays;

@Component
public class WebCrawlerConfig {

    private static final List<String> TOP_25_SECURITY_SITES = Arrays.asList(
        "https://krebsonsecurity.com",
        "https://www.schneier.com", // Cryptography
        "https://www.darkreading.com",
        "https://threatpost.com",
        "https://thehackernews.com",
        "https://www.bleepingcomputer.com",
        "https://www.wired.com/category/security/",
        "https://www.csoonline.com",
        "https://www.securityweek.com",
        "https://isc.sans.edu",
        "https://portswigger.net/daily-swig", // AppSec
        "https://owasp.org/blog/", // AppSec
        "https://www.infosecurity-magazine.com",
        "https://www.cyberscoop.com",
        "https://www.helpnetsecurity.com",
        "https://nakedsecurity.sophos.com",
        "https://www.malwarebytes.com/blog",
        "https://www.troyhunt.com",
        "https://www.eff.org",
        "https://googleprojectzero.blogspot.com",
        "https://www.microsoft.com/en-us/security/blog/",
        "https://openai.com/research", // AI Security
        "https://www.anthropic.com/research", // AI Security
        "https://www.hackerone.com/blog", // AppSec
        "https://www.bugcrowd.com/blog" // AppSec
    );

    @Tool(description = "Returns a curated list of the top 25 websites for researching Cryptography, Application Security, AI Security, and Mobile Security.")
    public List<String> getTopSecuritySites() {
        System.out.println("Researcher Agent retrieving top security sites list...");
        return TOP_25_SECURITY_SITES;
    }

    @Tool(description = "Searches the web for a given topic and returns a list of relevant URLs.")
    public List<String> searchWeb(String query) {
        System.out.println("Researcher Agent searching web for: " + query);
        List<String> urls = new java.util.ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(query, "UTF-8"))
                    .userAgent("Mozilla/5.0 Spring AI Agent")
                    .timeout(5000)
                    .get();
            org.jsoup.select.Elements links = doc.select("a.result__url");
            for (org.jsoup.nodes.Element link : links) {
                String href = link.attr("href");
                if (href != null && href.startsWith("http")) {
                    urls.add(href);
                }
            }
            if (urls.isEmpty()) {
                System.out.println("Search returned no URLs, falling back to top security sites.");
                return TOP_25_SECURITY_SITES;
            }
        } catch (Exception e) {
            System.out.println("Researcher Agent failed to search: " + e.getMessage());
            return TOP_25_SECURITY_SITES;
        }
        return urls;
    }

    @Tool(description = "Crawls a list of websites and extracts their text content. Use this to read the contents of URLs you decide are relevant.")
    public String crawl(List<String> urls) {
        System.out.println("Researcher Agent starting to crawl " + urls.size() + " URLs...");
        StringBuilder allContent = new StringBuilder();
        for (String url : urls) {
            try {
                System.out.println("Researcher Agent crawling: " + url);
                Document doc = Jsoup.connect(url)
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
