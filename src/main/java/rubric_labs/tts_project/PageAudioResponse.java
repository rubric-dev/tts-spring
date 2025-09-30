package rubric_labs.tts_project;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageAudioResponse {
    private int totalPages;
    private List<PageAudio> pages;

    @Data
    @AllArgsConstructor
    public static class PageAudio {
        private int pageNumber;
        private String imageBase64;  // PNG 이미지를 Base64로 인코딩
        private String audioBase64;  // MP3 오디오를 Base64로 인코딩
    }
}