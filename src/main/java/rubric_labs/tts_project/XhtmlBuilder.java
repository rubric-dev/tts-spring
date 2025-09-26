package rubric_labs.tts_project;

import java.util.List;

public class XhtmlBuilder {

    public String buildChapterXhtml(String title, List<ParagraphSegment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                  <head>
                    <title>
                """).append(escape(title)).append("""
                    </title>
                    <meta charset="UTF-8"/>
                  </head>
                  <body>
                """);

        for (ParagraphSegment seg : segments) {
            sb.append("<p>");
            List<String> sentences = seg.getSentences();
            for (int i = 0; i < sentences.size(); i++) {
                String id = "s" + seg.getIndex() + "_" + (i + 1);
                sb.append("<span id=\"").append(id).append("\" epub:type=\"bodymatter\">")
                        .append(escape(sentences.get(i)))
                        .append("</span> ");
            }
            sb.append("</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
