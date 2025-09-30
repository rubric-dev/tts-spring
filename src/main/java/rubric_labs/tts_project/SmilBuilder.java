package rubric_labs.tts_project;

import java.util.ArrayList;
import java.util.List;

public class SmilBuilder {

    /**
     * ✅ 페이지별로 개별 SMIL 파일 생성
     */
    public List<PageSmil> buildPageSmils(
            List<ParagraphSegment> segments,
            List<ParagraphTts> ttsList,
            String audioDir
    ) {
        List<PageSmil> pageSmils = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            ParagraphSegment seg = segments.get(i);
            int pageNum = seg.getIndex();
            String fileName = "page" + pageNum + ".smil";
            String xhtmlPath = "../text/page" + pageNum + ".xhtml";
            String audioFile = "../" + audioDir + "/chap1_p" + pageNum + ".mp3";

            String content = buildSinglePageSmil(xhtmlPath, pageNum, audioFile, seg);
            pageSmils.add(new PageSmil(fileName, content));
        }

        return pageSmils;
    }

    /**
     * ✅ 개별 페이지 SMIL 생성
     */
    private String buildSinglePageSmil(String xhtmlPath, int pageNum, String audioFile, ParagraphSegment segment) {
        StringBuilder sb = new StringBuilder();
        sb.append("<smil xmlns=\"http://www.w3.org/ns/SMIL\" xmlns:epub=\"http://www.idpf.org/2007/ops\" version=\"3.0\">");
        sb.append("<body>");
        sb.append("<seq id=\"id").append(pageNum).append("\" epub:textref=\"").append(xhtmlPath).append("\">");

        // 페이지 이미지와 오디오 연결
        sb.append("<par id=\"p").append(pageNum).append("\">");
        sb.append("<text src=\"").append(xhtmlPath).append("#page").append(pageNum).append("\"/>");
        sb.append("<audio src=\"").append(audioFile).append("\"/>");
        sb.append("</par>");

        // 문장별 동기화 (선택사항 - 단어별 하이라이트용)
        if (segment.sentences() != null && !segment.sentences().isEmpty()) {
            for (int i = 0; i < segment.sentences().size(); i++) {
                sb.append("<par id=\"s").append(i + 1).append("\">");
                sb.append("<text src=\"").append(xhtmlPath).append("#s").append(i + 1).append("\"/>");
                // 실제로는 각 문장별 오디오 타임스탬프 필요
                sb.append("</par>");
            }
        }

        sb.append("</seq>");
        sb.append("</body>");
        sb.append("</smil>");

        return sb.toString();
    }

    /**
     * 기존 메서드 (하위 호환성 유지)
     * @deprecated buildPageSmils() 사용 권장
     */
    @Deprecated
    public String buildChapterSmil(String chapterXhtmlPath,
                                   List<ParagraphSegment> segments,
                                   List<ParagraphTts> ttsList,
                                   String audioDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("<smil xmlns=\"http://www.w3.org/ns/SMIL\" xmlns:epub=\"http://www.idpf.org/2007/ops\" version=\"3.0\">");
        sb.append("<body><seq epub:textref=\"").append(chapterXhtmlPath).append("\">");

        // 페이지별로 이미지와 오디오 연결
        for (int i = 0; i < segments.size(); i++) {
            ParagraphSegment seg = segments.get(i);
            int pageNum = seg.getIndex();
            String audioFile = audioDir + "/chap1_p" + pageNum + ".mp3";
            String pageId = "page" + pageNum;

            sb.append("<par id=\"p").append(pageNum).append("\">");
            sb.append("<text src=\"").append(chapterXhtmlPath).append("#").append(pageId).append("\"/>");
            sb.append("<audio src=\"").append(audioFile).append("\"/>");
            sb.append("</par>");
        }

        sb.append("</seq></body></smil>");
        return sb.toString();
    }

    /**
     * 페이지 SMIL 데이터 클래스
     */
    public static class PageSmil {
        private final String fileName;
        private final String content;

        public PageSmil(String fileName, String content) {
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