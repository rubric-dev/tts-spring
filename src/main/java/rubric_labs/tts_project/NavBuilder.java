package rubric_labs.tts_project;

public class NavBuilder {

    public String buildNav(String bookTitle, int imageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<html xmlns=\"http://www.w3.org/1999/xhtml\"\n")
          .append("      xmlns:epub=\"http://www.idpf.org/2007/ops\">\n")
          .append("  <head>\n")
          .append("    <title>").append(escape(bookTitle)).append(" - Navigation</title>\n")
          .append("    <meta charset=\"UTF-8\"/>\n")
          .append("    <style type=\"text/css\">\n")
          .append("      nav ol { list-style-type: none; }\n")
          .append("      nav ol li { margin: 0.5em 0; }\n")
          .append("      nav ol li a { text-decoration: none; color: #0066cc; }\n")
          .append("      nav ol li a:hover { text-decoration: underline; }\n")
          .append("    </style>\n")
          .append("  </head>\n")
          .append("  <body>\n")
          .append("    <nav epub:type=\"toc\" id=\"toc\">\n")
          .append("      <h1>목차</h1>\n")
          .append("      <ol>\n")
          .append("        <li><a href=\"text/chap1.xhtml\">").append(escape(bookTitle)).append("</a></li>\n");
        for (int i = 1; i <= imageCount; i++) {
            sb.append("        <li><a href=\"images/page-").append(i).append(".png\">Page ").append(i).append("</a></li>\n");
        }
        sb.append("      </ol>\n")
          .append("    </nav>\n")
          .append("  </body>\n")
          .append("</html>\n");
        return sb.toString();
    }

    // 오버로드된 메서드 (기존 호환성 유지)
    public String buildNav() {
        return buildNav("Chapter 1", 0);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}