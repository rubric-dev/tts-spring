package rubric_labs.tts_project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OpfBuilder {

    public String buildOpf(String bookId, String title, String author, String language, int paragraphCount, int imageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"BookId\">")
                .append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">")

                // 기본 메타데이터
                .append("<dc:identifier id=\"BookId\">").append(escape(bookId)).append("</dc:identifier>")
                .append("<dc:title>").append(escape(title)).append("</dc:title>")
                .append("<dc:creator>").append(escape(author)).append("</dc:creator>")
                .append("<dc:language>").append(escape(language)).append("</dc:language>")

                // 발행일 (현재 날짜)
                .append("<dc:date>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</dc:date>")

                // 어린이 도서 메타데이터
                .append("<dc:subject>Children's Book</dc:subject>")
                .append("<dc:subject>Picture Book</dc:subject>")

                // 미디어 오버레이 지원 표시
                .append("<meta property=\"media:active-class\">-epub-media-overlay-active</meta>")
                .append("<meta property=\"media:playback-active-class\">-epub-media-overlay-playing</meta>")

                .append("</metadata>")

                .append("<manifest>")
                // 네비게이션 파일
                .append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>");

        // ✅ 페이지별 XHTML 파일 (미디어 오버레이 연결)
        for (int i = 1; i <= imageCount; i++) {
            sb.append("<item id=\"page").append(i)
                    .append("\" href=\"text/page").append(i).append(".xhtml\" media-type=\"application/xhtml+xml\"")
                    .append(" media-overlay=\"smil").append(i).append("\"/>");
        }

        // ✅ 페이지별 SMIL 파일
        for (int i = 1; i <= imageCount; i++) {
            sb.append("<item id=\"smil").append(i)
                    .append("\" href=\"smil/page").append(i).append(".smil\" media-type=\"application/smil+xml\"/>");
        }

        // 오디오 파일들
        for (int i = 1; i <= paragraphCount; i++) {
            sb.append("<item id=\"audio").append(i)
                    .append("\" href=\"audio/chap1_p").append(i).append(".mp3\" media-type=\"audio/mpeg\"/>");
        }

        // 이미지 파일들
        for (int i = 1; i <= imageCount; i++) {
            sb.append("<item id=\"img").append(i)
                    .append("\" href=\"images/page-").append(i)
                    .append(".png\" media-type=\"image/png\"/>");
        }

        sb.append("</manifest>");

        // ✅ 페이지별 spine 생성
        sb.append("<spine>");
        for (int i = 1; i <= imageCount; i++) {
            sb.append("<itemref idref=\"page").append(i).append("\"/>");
        }
        sb.append("</spine>");

        // 가이드 (선택사항)
        sb.append("<guide>")
                .append("<reference type=\"text\" title=\"Start\" href=\"text/page1.xhtml\"/>")
                .append("</guide>")

                .append("</package>");

        return sb.toString();
    }

    // 오버로드된 메서드 (기존 호환성 유지)
    public String buildOpf(String bookId, String title, String language, int paragraphCount, int imageCount) {
        return buildOpf(bookId, title, "Unknown Author", language, paragraphCount, imageCount);
    }

    public String buildContainerXml() {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
        """;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}