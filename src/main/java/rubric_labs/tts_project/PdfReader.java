package rubric_labs.tts_project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReader {

    private final PdfMetadataExtractor metadataExtractor;

    public String getSsmlFromPdfFile(MultipartFile file) {
        BookMetadata metadata = metadataExtractor.extractMetadata(file);
        List<String> cleanedPages = extractCleanText(file, metadata);
        String result = buildSSML(cleanedPages, metadata);

        log.info("Polly SSML request length={} for book: {}", result.length(), metadata.getTitle());
        return result;
    }

    public List<ParagraphSegment> extractParagraphs(MultipartFile file) {
        BookMetadata metadata = metadataExtractor.extractMetadata(file);
        List<String> cleanedBlocks = extractCleanText(file, metadata);

        List<ParagraphSegment> segments = new ArrayList<>();
        int idx = 1;
        for (String block : cleanedBlocks) {
            String safe = sanitize(block, metadata.getLanguage());
            List<String> sentences = splitSentences(safe, metadata.getLanguage());

            String ssml = buildParagraphSSML(sentences, metadata);
            segments.add(new ParagraphSegment(idx++, sentences, ssml));
        }
        return segments;
    }

    private List<String> extractCleanText(MultipartFile file, BookMetadata metadata) {
        List<String> cleanedPages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String rawText = stripper.getText(document);

                // 페이지 필터링 (언어별 다른 규칙 적용)
                if (shouldSkipPage(rawText, page, pageCount, metadata.getLanguage())) {
                    continue;
                }

                String filteredText = filterContent(rawText, metadata.getLanguage());
                if (!filteredText.isBlank()) {
                    cleanedPages.add(filteredText.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("PDF 읽기 실패: " + e.getMessage(), e);
        }

        return cleanedPages;
    }

    private boolean shouldSkipPage(String rawText, int page, int pageCount, String language) {
        String normalized = rawText.toLowerCase().replaceAll("\\s+", "");

        // 첫 페이지나 마지막 페이지의 판권/ISBN 페이지 제거
        if ((page == 1 || page == pageCount) && containsBoilerplate(normalized, language)) {
            return true;
        }

        // 언어별 특별 처리
        switch (language) {
            case "ko":
                return containsKoreanBoilerplate(normalized);
            case "ja":
                return containsJapaneseBoilerplate(normalized);
            case "zh":
                return containsChineseBoilerplate(normalized);
            default:
                return containsEnglishBoilerplate(normalized);
        }
    }

    private boolean containsBoilerplate(String text, String language) {
        // 공통 판권 정보
        if (text.contains("isbn") || text.contains("copyright") || text.contains("©")) {
            return true;
        }

        return switch (language) {
            case "ko" -> containsKoreanBoilerplate(text);
            case "ja" -> containsJapaneseBoilerplate(text);
            case "zh" -> containsChineseBoilerplate(text);
            default -> containsEnglishBoilerplate(text);
        };
    }

    private boolean containsEnglishBoilerplate(String text) {
        String[] boilerplatePatterns = {
                "firstpublishedin", "allrightsreserved", "britishlibrary",
                "franklin watts", "hachette", "reading champion",
                "printed in", "www."
        };

        for (String pattern : boilerplatePatterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKoreanBoilerplate(String text) {
        String[] koreanBoilerplate = {
                "저작권", "판권소유", "무단전재", "복제금지",
                "발행일", "발행처", "출판사", "인쇄처"
        };

        for (String pattern : koreanBoilerplate) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsJapaneseBoilerplate(String text) {
        String[] japaneseBoilerplate = {
                "著作権", "版権", "無断転載", "複製禁止",
                "発行日", "出版社", "印刷所"
        };

        for (String pattern : japaneseBoilerplate) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsChineseBoilerplate(String text) {
        String[] chineseBoilerplate = {
                "版权", "著作权", "出版社", "印刷",
                "发行", "禁止复制"
        };

        for (String pattern : chineseBoilerplate) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String filterContent(String text, String language) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("^[0-9]+$")) // 페이지 번호 제거
                .filter(line -> !isBoilerplateLine(line, language))
                .collect(Collectors.joining("\n"));
    }

    private boolean isBoilerplateLine(String line, String language) {
        String lower = line.toLowerCase();

        // 공통 필터
        if (lower.contains("copyright") || lower.contains("www.") ||
                lower.matches("^page \\d+$") || lower.length() < 2) {
            return true;
        }

        // 언어별 필터
        return switch (language) {
            case "ko" -> isKoreanBoilerplateLine(lower);
            case "ja" -> isJapaneseBoilerplateLine(lower);
            case "zh" -> isChineseBoilerplateLine(lower);
            default -> isEnglishBoilerplateLine(lower);
        };
    }

    private boolean isEnglishBoilerplateLine(String line) {
        return line.contains("printed in") ||
                line.contains("franklin watts") ||
                line.contains("hachette") ||
                line.contains("reading champion");
    }

    private boolean isKoreanBoilerplateLine(String line) {
        return line.contains("저작권") || line.contains("출판사") || line.contains("발행");
    }

    private boolean isJapaneseBoilerplateLine(String line) {
        return line.contains("著作権") || line.contains("出版社") || line.contains("発行");
    }

    private boolean isChineseBoilerplateLine(String line) {
        return line.contains("版权") || line.contains("出版社") || line.contains("发行");
    }

    private String buildSSML(List<String> contents, BookMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("<speak>");

        for (String block : contents) {
            String safe = sanitize(block, metadata.getLanguage());
            List<String> sentences = splitSentences(safe, metadata.getLanguage());

            sb.append("<p>");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (!trimmed.isBlank()) {
                    sb.append("<s>").append(trimmed).append("</s>");
                }
            }
            sb.append("</p>");
            sb.append("<break time=\"1s\"/>");
        }

        sb.append("</speak>");
        return sb.toString();
    }

    private String buildParagraphSSML(List<String> sentences, BookMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("<speak><p>");
        for (String s : sentences) {
            String trimmed = s.trim();
            if (!trimmed.isBlank()) {
                sb.append("<s>").append(trimmed).append("</s>");
            }
        }
        sb.append("</p></speak>");
        return sb.toString();
    }

    private String sanitize(String text, String language) {
        String sanitized = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // 언어별 인용부호 정리
        switch (language) {
            case "ko":
                sanitized = sanitized.replace("\u201C", "\"").replace("\u201D", "\"")
                        .replace("\u2018", "'").replace("\u2019", "'");
                break;
            case "en":
                sanitized = sanitized.replace("\u201C", "\"").replace("\u201D", "\"")
                        .replace("\u2018", "'").replace("\u2019", "'");
                break;
            // 일본어, 중국어는 기본 처리
        }

        return sanitized.replaceAll("\\.\\.", ".").replace("\n", " ");
    }

    private List<String> splitSentences(String text, String language) {
        String sentencePattern = switch (language) {
            case "ko" -> "(?<=[.!?。！？])\\s+";
            case "ja" -> "(?<=[.!?。！？])\\s+";
            case "zh" -> "(?<=[.!?。！？])\\s+";
            default -> "(?<=[.!?])\\s+";
        };

        return Arrays.stream(text.split(sentencePattern))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}