package com.yash.nerve.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WebContentTool {

    @Tool(description = """
            Read the content of a webpage.
            Provide a URL from a search result.
            Returns cleaned text content.
            """)
    public String extractContent(String url) {

        try {

            Document doc =
                    Jsoup.connect(url)
                            .userAgent(
                                    "Mozilla/5.0"
                            )
                            .timeout(10000)
                            .get();

            doc.select(
                    "script,style,noscript,header,footer,nav"
            ).remove();

            String title = doc.title();

            String content =
                    doc.body().text();

            if (content.length() > 12000) {
                content =
                        content.substring(0, 12000);
            }

            return """
                    TITLE:
                    %s

                    CONTENT:
                    %s
                    """
                    .formatted(
                            title,
                            content
                    );

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}