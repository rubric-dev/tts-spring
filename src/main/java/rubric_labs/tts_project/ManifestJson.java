package rubric_labs.tts_project;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * manifest.json 구조
 */
@Data
public class ManifestJson {
    private String bookId;
    private String title;
    private String author;
    private String language;
    private String epubUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createdAt;

    private int totalPages;
    private List<ManifestPage> pages;
}

/**
 * 개별 페이지 정보
 */
@Data
class ManifestPage {
    private int pageNumber;
    private String imageUrl;
    private String audioUrl;
    private String text;
}