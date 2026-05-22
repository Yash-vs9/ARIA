package com.yash.nerve.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleSearchTool {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();

    @Value("${serpapi.api-key}")
    private String apiKey;

    @Tool(description = """
            Search Google for information.
            Use this when you need current information from the web.
            Returns top search results including title, snippet and URL.
            """)
    public String searchGoogle(String query) {

        try {

            String encodedQuery =
                    URLEncoder.encode(
                            query,
                            StandardCharsets.UTF_8
                    );

            String url =
                    "https://serpapi.com/search.json"
                            + "?engine=google"
                            + "&q=" + encodedQuery
                            + "&api_key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response =
                         client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    return "ERROR: Search request failed";
                }

                String json =
                        response.body().string();

                JsonNode root =
                        objectMapper.readTree(json);

                JsonNode results =
                        root.path("organic_results");

                if (results.isMissingNode()
                        || results.isEmpty()) {

                    return "No search results found.";
                }

                StringBuilder output =
                        new StringBuilder();

                int count = 0;

                for (JsonNode result : results) {

                    if (count >= 5) {
                        break;
                    }

                    output.append("Title: ")
                            .append(result.path("title").asText())
                            .append("\n");

                    output.append("Link: ")
                            .append(result.path("link").asText())
                            .append("\n");

                    output.append("Snippet: ")
                            .append(result.path("snippet").asText())
                            .append("\n\n");

                    count++;
                }

                return output.toString();
            }

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
