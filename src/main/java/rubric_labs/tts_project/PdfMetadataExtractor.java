package rubric_labs.tts_project;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfMetadataExtractor {

    public BookMetadata extractMetadata(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            // 1. PDF 메타데이터에서 정보 추출
            PDDocumentInformation info = document.getDocumentInformation();

            String title = extractTitle(info, document);
            String author = extractAuthor(info);
            String language = detectLanguage(document);
            String voice = selectVoiceByLanguage(language);
            String bookId = generateBookId(title);

            BookMetadata metadata = new BookMetadata(title, author, language, voice, bookId);
            log.info("추출된 메타데이터: {}", metadata);
            return metadata;

        } catch (IOException e) {
            log.error("PDF 메타데이터 추출 실패", e);
            return BookMetadata.createDefault(file.getOriginalFilename());
        }
    }

    private String extractTitle(PDDocumentInformation info, PDDocument document) throws IOException {
        // 1. PDF 메타데이터에서 제목 확인
        if (info.getTitle() != null && !info.getTitle().trim().isEmpty()) {
            return info.getTitle().trim();
        }

        // 2. 첫 페이지에서 제목 추출
        return extractTitleFromFirstPage(document);
    }

    private String extractTitleFromFirstPage(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(1);

        String firstPageText = stripper.getText(document);
        String[] lines = firstPageText.split("\n");

        // 첫 번째 의미있는 텍스트를 제목으로 간주
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() &&
                    !trimmed.matches("^[0-9]+$") &&
                    !isCommonBoilerplate(trimmed) &&
                    trimmed.length() > 3 &&
                    trimmed.length() < 100) {
                return trimmed;
            }
        }

        return "Unknown Title";
    }

    private String extractAuthor(PDDocumentInformation info) {
        if (info.getAuthor() != null && !info.getAuthor().trim().isEmpty()) {
            return info.getAuthor().trim();
        }
        return "Unknown Author";
    }

    private String detectLanguage(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(3, document.getNumberOfPages())); // 처음 3페이지만 확인

        String sampleText = stripper.getText(document).toLowerCase();

        // 한글 감지
        if (containsKorean(sampleText)) {
            return "ko";
        }

        // 일본어 감지
        if (containsJapanese(sampleText)) {
            return "ja";
        }

        // 중국어 감지 (간체)
        if (containsChinese(sampleText)) {
            return "zh";
        }

        // 영어 일반 단어들
        String[] englishWords = {"the", "and", "is", "was", "are", "were", "have", "has", "had", "will", "would", "this", "that"};
        int englishCount = 0;
        for (String word : englishWords) {
            if (sampleText.contains(" " + word + " ") || sampleText.startsWith(word + " ")) {
                englishCount++;
            }
        }

        if (englishCount >= 3) {
            return "en";
        }

        // 기본값은 영어
        return "en";
    }

    private boolean containsKorean(String text) {
        Pattern korean = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣]");
        return korean.matcher(text).find();
    }

    private boolean containsJapanese(String text) {
        Pattern japanese = Pattern.compile("[ひらがなカタカナ一-龯]");
        return japanese.matcher(text).find();
    }

    private boolean containsChinese(String text) {
        Pattern chinese = Pattern.compile("[一-龯]");
        return chinese.matcher(text).find();
    }

    private String selectVoiceByLanguage(String language) {
        return switch (language) {
            case "ko" -> "Seoyeon";    // AWS Polly 한국어 음성
            case "ja" -> "Mizuki";     // AWS Polly 일본어 음성
            case "zh" -> "Zhiyu";      // AWS Polly 중국어 음성
            case "en" -> "Joanna";     // AWS Polly 영어 음성 (어린이 책에 적합)
            default -> "Joanna";
        };
    }

    private String generateBookId(String title) {
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9가-힣]", "").toLowerCase();
        if (cleanTitle.length() > 20) {
            cleanTitle = cleanTitle.substring(0, 20);
        }
        return "urn:uuid:" + cleanTitle + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isCommonBoilerplate(String text) {
        String lower = text.toLowerCase();
        return lower.contains("copyright") ||
                lower.contains("printed in") ||
                lower.contains("isbn") ||
                lower.contains("www.") ||
                lower.contains("all rights reserved") ||
                lower.matches("^page \\d+$") ||
                lower.matches("^\\d+$");
    }
}