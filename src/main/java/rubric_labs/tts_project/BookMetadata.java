package rubric_labs.tts_project;

import lombok.Data;

@Data
public class BookMetadata {
    private String title;
    private String author;
    private String language;
    private String bookId;
    private String voice;
    private boolean isChildrenBook = true;

    public BookMetadata() {}

    public BookMetadata(String title, String author, String language, String voice, String bookId) {
        this.title = title;
        this.author = author;
        this.language = language;
        this.voice = voice;
        this.bookId = bookId;
    }

    // 기본값 설정 메서드
    public static BookMetadata createDefault(String filename) {
        BookMetadata metadata = new BookMetadata();
        metadata.setTitle(filename != null ? filename.replaceAll("\\.[^.]*$", "") : "Unknown Book");
        metadata.setAuthor("Unknown Author");
        metadata.setLanguage("en");
        metadata.setVoice("Joanna");
        metadata.setBookId("urn:uuid:default-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        return metadata;
    }
}