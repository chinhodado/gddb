package com.chin.gddb;

import java.util.Locale;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class HtmlCleaner {
    static Element getCleanedHtml(Element content) {
        content.select("script").remove();               // remove <script> tags
        content.select("noscript").remove();             // remove <noscript> tags
        content.select("#toc").remove();                 // remove the table of content
        content.select("sup").remove();                  // remove the sup tags
        content.select("table.collapsible").remove();    // ending categories table
        content.select(".article-thumb").remove();       // in-article images
        content.select("#Gunpla").remove();              // gunpla section header
        content.select("#stub").remove();                // wiki stub notice
        content.select("#cleanup").remove();             // wiki cleanup notice
        content.select(".box.message").remove();         // gundam wiki message boxes
        removeComments(content);                         // remove comments

        // remove gallery header and anything from the reference section onward
        Elements h2s = content.select("h2");
        if (!h2s.isEmpty()) {
            for (Element h2 : h2s) {
                String title = h2.text().toUpperCase(Locale.US);
                if (title.contains("GALLERY")) {
                    h2.remove();
                }
                else if (title.contains("REFERENCES")) {
                    Element parent = h2.parent();
                    Elements children = parent.children();
                    int index = children.indexOf(h2);
                    for (int i = index; i < children.size(); i++) {
                        children.get(i).remove();
                    }
                }
            }
        }

        // remove picture galleries
        content.select("div[id^=gallery]").remove();

        return content;
    }

    private static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }
}
