package rubric_labs.tts_project;

import java.util.List;

public class SmilBuilder {

    /**
     * 페이지별 mp3와 이미지를 연결하는 SMIL 생성
     */
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
}
