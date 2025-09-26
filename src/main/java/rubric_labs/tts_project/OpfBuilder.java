package rubric_labs.tts_project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OpfBuilder {

    public String buildOpf(String bookId, String title, String author, String language, int paragraphCount) {
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
                .append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")

                // 챕터 파일 (미디어 오버레이 연결)
                .append("<item id=\"chap1\" href=\"text/chap1.xhtml\" media-type=\"application/xhtml+xml\" media-overlay=\"smil1\"/>")

                // SMIL 파일
                .append("<item id=\"smil1\" href=\"smil/chap1.smil\" media-type=\"application/smil+xml\"/>");

        // 오디오 파일들
        for (int i = 1; i <= paragraphCount; i++) {
            sb.append("<item id=\"audio").append(i)
                    .append("\" href=\"audio/chap1_p").append(i).append(".mp3\" media-type=\"audio/mpeg\"/>");
        }

        sb.append("</manifest>")

                // 읽기 순서
                .append("<spine>")
                .append("<itemref idref=\"chap1\"/>")
                .append("</spine>")

                // 가이드 (선택사항)
                .append("<guide>")
                .append("<reference type=\"text\" title=\"Start\" href=\"text/chap1.xhtml\"/>")
                .append("</guide>")

                .append("</package>");

        return sb.toString();
    }

    // 오버로드된 메서드 (기존 호환성 유지)
    public String buildOpf(String bookId, String title, String language, int paragraphCount) {
        return buildOpf(bookId, title, "Unknown Author", language, paragraphCount);
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