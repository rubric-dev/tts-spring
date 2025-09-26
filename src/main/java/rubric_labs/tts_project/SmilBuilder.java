package rubric_labs.tts_project;

import java.util.List;

public class SmilBuilder {

    /**
     * 문단별 mp3와 문장 시작시각(ms)을 사용해 SMIL 생성.
     * clipEnd는 다음 문장 시작시각 - 0.01s 를 사용.
     * 문단의 마지막 문장은 clipEnd 생략(해당 mp3 끝까지 재생).
     */
    public String buildChapterSmil(String chapterXhtmlPath,
                                   List<ParagraphSegment> segments,
                                   List<ParagraphTts> ttsList,
                                   String audioDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("<smil xmlns=\"http://www.w3.org/ns/SMIL\" version=\"3.0\"><body><seq>");

        // paragraph index를 키로 빠른 접근
        ParagraphTts[] byIndex = new ParagraphTts[segments.size() + 1];
        for (ParagraphTts t : ttsList) byIndex[t.getParagraphIndex()] = t;

        for (ParagraphSegment seg : segments) {
            ParagraphTts tts = byIndex[seg.getIndex()];
            String audioFile = audioDir + "/chap1_p" + seg.getIndex() + ".mp3";

            List<String> sentences = seg.getSentences();
            List<SpeechMark> marks = tts.getMarks();

            for (int i = 0; i < sentences.size(); i++) {
                String textId = "s" + seg.getIndex() + "_" + (i + 1);
                double beginSec = marks.get(i).getTime() / 1000.0;
                String clipBegin = String.format("%.3fs", beginSec);

                String clipEnd = null;
                if (i + 1 < marks.size()) {
                    double next = marks.get(i + 1).getTime() / 1000.0;
                    double end = Math.max(beginSec, next - 0.010);
                    clipEnd = String.format("%.3fs", end);
                }
                // If clipEnd is null (last sentence), use total duration of audio file for paragraph
                if (clipEnd == null && !marks.isEmpty()) {
                    double last = marks.get(marks.size() - 1).getTime() / 1000.0;
                    clipEnd = String.format("%.3fs", last);
                }

                sb.append("<par>");
                sb.append("<text src=\"").append(chapterXhtmlPath).append("#").append(textId).append("\"/>");
                sb.append("<audio src=\"").append(audioFile)
                  .append("\" clipBegin=\"").append(clipBegin)
                  .append("\" clipEnd=\"").append(clipEnd).append("\"");
                sb.append("/>");
                sb.append("</par>");
            }
        }

        sb.append("</seq></body></smil>");
        return sb.toString();
    }
}
