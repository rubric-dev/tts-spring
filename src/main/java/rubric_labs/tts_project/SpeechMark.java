package rubric_labs.tts_project;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SpeechMark {
    // Polly JSON 예: {"time":234,"type":"sentence","start":0,"end":12,"value":"This bird can fly."}
    private long time;     // ms
    private String type;   // "sentence"
    private int start;
    private int end;
    private String value;

    public SpeechMark() {}
    
    public SpeechMark(String type, int start, int end, long time, String value) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.time = time;
        this.value = value;
    }

}
