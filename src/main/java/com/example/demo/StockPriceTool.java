package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.springframework.ai.tool.annotation.Tool;

public class StockPriceTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Tool(description = "Fetches the current stock price and 52-week trending (high/low) for a given Canadian stock on the Toronto Stock Exchange (TSE/TSX). Input should be the ticker symbol (e.g. SHOP, RY, TD).")
    public String getStockPrice(String ticker) {
        // Yahoo Finance uses .TO suffix for Toronto Stock Exchange
        if (!ticker.toUpperCase().endsWith(".TO")) {
            ticker = ticker.toUpperCase() + ".TO";
        }
        
        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker;
            
            // We use Jsoup to bypass simple User-Agent blocks, then Jackson to parse the JSON
            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get()
                    .body()
                    .text();
            
            JsonNode root = mapper.readTree(json);
            double price = root.at("/chart/result/0/meta/regularMarketPrice").asDouble();
            double fiftyTwoWeekHigh = root.at("/chart/result/0/meta/fiftyTwoWeekHigh").asDouble();
            double fiftyTwoWeekLow = root.at("/chart/result/0/meta/fiftyTwoWeekLow").asDouble();
            
            return "The current price for " + ticker + " is $" + price + " CAD. " +
                   "52-week High: $" + fiftyTwoWeekHigh + " CAD, " +
                   "52-week Low: $" + fiftyTwoWeekLow + " CAD.";
        } catch (Exception e) {
            return "Could not fetch stock price for " + ticker + ". Please ensure it is a valid Canadian ticker symbol.";
        }
    }
}
