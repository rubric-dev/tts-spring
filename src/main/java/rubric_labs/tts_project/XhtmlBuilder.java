package rubric_labs.tts_project;

import java.util.List;

public class XhtmlBuilder {

    public String buildChapterXhtml(String title, List<ParagraphSegment> segments, int imageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                  <head>
                    <title>
                """).append(escape(title)).append("""
                    </title>
                    <meta charset="UTF-8"/>
                    <style>
                      .page-image { 
                        cursor: pointer; 
                        margin: 20px 0;
                        text-align: center;
                      }
                      .page-image img { 
                        max-width: 100%; 
                        height: auto;
                      }
                    </style>
                  </head>
                  <body>
                """);

        // 페이지별 이미지 + 오디오 연결
        for (int i = 1; i <= imageCount; i++) {
            sb.append("<div class=\"page-image\" id=\"page").append(i).append("\" epub:type=\"bodymatter\">\n")
              .append("  <img src=\"../images/page-").append(i).append(".png\" alt=\"Page ").append(i).append("\"/>\n")
              .append("</div>\n");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
