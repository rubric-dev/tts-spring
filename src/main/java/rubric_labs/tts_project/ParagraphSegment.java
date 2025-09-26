package rubric_labs.tts_project;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class ParagraphSegment {
    private final int index;
    private final List<String> sentences;
    private final String ssml;

    /**
     * @param index     1-based
     * @param sentences 정제된 문장 리스트
     * @param ssml      이 문단만 감싼 <speak><p><s>..</s>..</p></speak>
     */
    public ParagraphSegment(int index, List<String> sentences, String ssml) {
        this.index = index;
        this.sentences = sentences;
        this.ssml = ssml;
    }

    public int index() {
        return index;
    }

    public List<String> sentences() {
        return sentences;
    }

    public String ssml() {
        return ssml;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ParagraphSegment) obj;
        return this.index == that.index &&
                Objects.equals(this.sentences, that.sentences) &&
                Objects.equals(this.ssml, that.ssml);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, sentences, ssml);
    }

    @Override
    public String toString() {
        return "ParagraphSegment[" +
                "index=" + index + ", " +
                "sentences=" + sentences + ", " +
                "ssml=" + ssml + ']';
    }

}
