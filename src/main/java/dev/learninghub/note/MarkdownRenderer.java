package dev.learninghub.note;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.regex.Pattern;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Renders a note body to inert HTML (FR-5): raw HTML in the Markdown is escaped, unsafe URLs are dropped,
 * and the result is sanitised against an allow-list. Images are not allowed, so no external content is loaded.
 */
@ApplicationScoped
class MarkdownRenderer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "h1", "h2", "h3", "h4", "h5", "h6", "em", "strong", "code", "pre",
                    "blockquote", "ul", "ol", "li", "hr", "br", "a")
            .allowAttributes("href").onElements("a")
            .allowAttributes("start").matching(Pattern.compile("\\d{1,9}")).onElements("ol")
            .allowAttributes("class").matching(Pattern.compile("language-[\\w+#-]{1,30}")).onElements("code")
            .allowUrlProtocols("http", "https")
            .requireRelsOnLinks("nofollow", "noopener", "noreferrer")
            .toFactory();

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    String toSafeHtml(String markdown) {
        String html = POLICY.sanitize(renderer.render(parser.parse(markdown)));
        // The sanitizer over-encodes characters that are inert in text and quoted attributes; undo it for readability.
        return html.replace("&#61;", "=").replace("&#43;", "+").replace("&#64;", "@").replace("&#96;", "`");
    }
}
