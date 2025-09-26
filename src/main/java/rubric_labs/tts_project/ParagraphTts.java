package rubric_labs.tts_project;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class ParagraphTts {
    private final int paragraphIndex;
    private final byte[] mp3;
    private final List<SpeechMark> marks;

    public ParagraphTts(int paragraphIndex, byte[] mp3, List<SpeechMark> marks) {
        this.paragraphIndex = paragraphIndex;
        this.mp3 = mp3;
        this.marks = marks;
    }

    public int paragraphIndex() {
        return paragraphIndex;
    }

    public byte[] mp3() {
        return mp3;
    }

    public List<SpeechMark> marks() {
        return marks;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ParagraphTts) obj;
        return this.paragraphIndex == that.paragraphIndex &&
                Objects.equals(this.mp3, that.mp3) &&
                Objects.equals(this.marks, that.marks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paragraphIndex, mp3, marks);
    }

    @Override
    public String toString() {
        return "ParagraphTts[" +
                "paragraphIndex=" + paragraphIndex + ", " +
                "mp3=" + mp3 + ", " +
                "marks=" + marks + ']';
    }

}
