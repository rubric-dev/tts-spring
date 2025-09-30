package rubric_labs.tts_project;

import java.util.ArrayList;
import java.util.List;

public class XhtmlBuilder {

    /**
     * ✅ 페이지별로 개별 XHTML 파일 생성
     * @return List<PageXhtml> - 각 페이지의 파일명과 내용
     */
    public List<PageXhtml> buildPageXhtmls(String title, List<ParagraphSegment> segments, int imageCount) {
        List<PageXhtml> pages = new ArrayList<>();

        for (int i = 1; i <= imageCount; i++) {
            String fileName = "page" + i + ".xhtml";
            String content = buildSinglePageXhtml(title, i, segments.size() >= i ? segments.get(i - 1) : null);
            pages.add(new PageXhtml(fileName, content));
        }

        return pages;
    }

    /**
     * ✅ 개별 페이지 XHTML 생성
     */
    private String buildSinglePageXhtml(String title, int pageNumber, ParagraphSegment segment) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                  <head>
                    <title>
                """).append(escape(title)).append(" - Page ").append(pageNumber).append("""
                    </title>
                    <meta charset="UTF-8"/>
                    <style>
                      body {
                        margin: 0;
                        padding: 20px;
                        text-align: center;
                        background: #f5f5f5;
                      }
                      .page-container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: white;
                        padding: 20px;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                      }
                      .page-image { 
                        cursor: pointer; 
                        margin: 20px 0;
                      }
                      .page-image img { 
                        max-width: 100%; 
                        height: auto;
                        border-radius: 5px;
                      }
                      .page-text {
                        margin-top: 20px;
                        font-size: 18px;
                        line-height: 1.6;
                        color: #333;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="page-container">
                      <div class="page-image" id="page""").append(pageNumber).append("""
                " epub:type="bodymatter">
                        <img src="../images/page-""").append(pageNumber).append(".png\" alt=\"Page ").append(pageNumber).append("""
                "/>
                      </div>
                """);

        // 텍스트 추가 (있는 경우)
        if (segment != null && segment.sentences() != null && !segment.sentences().isEmpty()) {
            sb.append("<div class=\"page-text\" id=\"text").append(pageNumber).append("\">\n");
            for (int i = 0; i < segment.sentences().size(); i++) {
                sb.append("<span id=\"s").append(i + 1).append("\">")
                        .append(escape(segment.sentences().get(i)))
                        .append("</span> ");
            }
            sb.append("</div>\n");
        }

        sb.append("""
                    </div>
                  </body>
                </html>
                """);

        return sb.toString();
    }

    /**
     * 기존 호환성을 위한 메서드 (Deprecated)
     * @deprecated 페이지별 XHTML 생성을 위해 buildPageXhtmls() 사용 권장
     */
    @Deprecated
    public String buildChapterXhtml(String title, List<ParagraphSegment> segments, int imageCount) {
        // 모든 페이지를 하나의 파일에 넣는 구식 방법
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

    /**
     * 페이지 XHTML 데이터 클래스
     */
    public static class PageXhtml {
        private final String fileName;
        private final String content;

        public PageXhtml(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContent() {
            return content;
        }
    }
}