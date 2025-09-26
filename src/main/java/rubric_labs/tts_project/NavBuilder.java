package rubric_labs.tts_project;

public class NavBuilder {

    public String buildNav(String bookTitle) {
        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:epub="http://www.idpf.org/2007/ops">
                  <head>
                    <title>%s - Navigation</title>
                    <meta charset="UTF-8"/>
                    <style type="text/css">
                      nav ol { list-style-type: none; }
                      nav ol li { margin: 0.5em 0; }
                      nav ol li a { text-decoration: none; color: #0066cc; }
                      nav ol li a:hover { text-decoration: underline; }
                    </style>
                  </head>
                  <body>
                    <nav epub:type="toc" id="toc">
                      <h1>목차</h1>
                      <ol>
                        <li><a href="text/chap1.xhtml">%s</a></li>
                      </ol>
                    </nav>
                  </body>
                </html>
                """, escape(bookTitle), escape(bookTitle));
    }

    // 오버로드된 메서드 (기존 호환성 유지)
    public String buildNav() {
        return buildNav("Chapter 1");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}