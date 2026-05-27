package com.zak.jobhunter.enrichment;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * Extracts clean readable text from a parsed HTML {@link Document}.
 *
 * <p>Strips boilerplate elements (nav, header, footer, scripts, ads) and
 * returns the main body text suitable for passing to an AI model.
 */
@Component
public class ReadableTextExtractor {

    private static final String[] REMOVE_SELECTORS = {
            "script", "style", "noscript", "nav", "header", "footer",
            "iframe", "aside", "[class*=ad]", "[class*=cookie]",
            "[class*=banner]", "[class*=popup]", "[id*=ad]"
    };

    /**
     * Extract readable text from the document.
     *
     * @param doc      parsed Jsoup document
     * @param maxChars maximum number of characters to return
     * @return clean text content
     */
    public String extract(Document doc, int maxChars) {
        // Remove noise elements
        for (String selector : REMOVE_SELECTORS) {
            doc.select(selector).remove();
        }

        // Try to find the main content area
        String text = findMainContent(doc);

        // Fallback to full body text
        if (text == null || text.isBlank()) {
            text = doc.body() != null ? doc.body().text() : doc.text();
        }

        // Normalize whitespace
        text = text.replaceAll("\\s{3,}", "\n\n").strip();

        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }

    /**
     * Extract an "apply" link if present on the page.
     */
    public String extractApplyUrl(Document doc) {
        // Look for common apply button/link patterns
        Elements links = doc.select(
                "a[href*=apply], a:contains(Apply), a:contains(Apply now), " +
                "a:contains(Apply for), button:contains(Apply)");
        if (!links.isEmpty()) {
            Element link = links.first();
            String href = link.attr("abs:href");
            return href.isBlank() ? null : href;
        }
        return null;
    }

    private String findMainContent(Document doc) {
        // Try semantic / well-known content containers in priority order
        String[] candidates = {
                "main", "article", "[role=main]",
                ".job-description", ".description", ".content",
                "#job-description", "#description", "#content"
        };
        for (String selector : candidates) {
            Elements el = doc.select(selector);
            if (!el.isEmpty()) {
                String text = el.first().text();
                if (text.length() > 100) return text;
            }
        }
        return null;
    }
}
