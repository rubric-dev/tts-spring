package rubric_labs.tts_project;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RequestMapping("/tts")
@RestController
public class TteApi {

    @PostMapping(value = "", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void readPdf(@RequestPart("file") MultipartFile file) throws IOException {
        List<String> cleanedPages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String rawText = stripper.getText(document);
                String normalized = rawText
                        .toLowerCase()
                        .replaceAll("\\s+", "");  // 모든 공백 제거

                // 1) 첫 페이지 또는 마지막 페이지 → ISBN 포함 시 skip
                if ((page == 1 || page == pageCount) && normalized.contains("isbn")) {
                    System.out.println("===== PAGE " + page + " skipped (ISBN detected) =====");
                    continue;
                }

                // 2) 중간 판권부 페이지 감지 (키워드 기반)
                if (normalized.contains("firstpublishedin") ||
                        normalized.contains("allrightsreserved") ||
                        normalized.contains("britishlibrary")) {
                    System.out.println("===== PAGE " + page + " skipped (colophon detected) =====");
                    continue;
                }

                // 3) 나머지는 filterDummy 적용
                String filteredText = filterDummy(rawText);

                if (!filteredText.isBlank()) {
                    cleanedPages.add(filteredText.trim());
                }
            }
        }

        // === SSML 생성 ===
        String ssml = buildSSML(cleanedPages);

        System.out.println("===== GENERATED SSML =====");
        System.out.println(ssml);
        System.out.println("==========================");
    }

    /**
     * 더미 텍스트 줄 단위 필터링
     */
    private String filterDummy(String text) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("^[0-9]+$")) // 숫자만 있는 줄 제거
                .filter(line -> !line.toLowerCase().contains("copyright"))
                .filter(line -> !line.toLowerCase().contains("printed in"))
                .filter(line -> !line.toLowerCase().contains("franklin watts"))
                .filter(line -> !line.toLowerCase().contains("hachette"))
                .filter(line -> !line.toLowerCase().contains("www."))
                .filter(line -> !line.toLowerCase().contains("reading champion"))
                .collect(Collectors.joining("\n"));
    }

    private String buildSSML(List<String> contents) {
        StringBuilder sb = new StringBuilder();
        sb.append("<speak>");

        for (String block : contents) {
            String lower = block.toLowerCase();

            // 본문 문장 ("This ... can fly.")
            if (lower.startsWith("this")) {
                sb.append("<p>").append(block).append("</p>");
                sb.append("<break time=\"1s\"/>");
            }
            // 가이드 헤더 + 본문
            else if (lower.startsWith("talk and explore") ||
                    lower.startsWith("independent reading") ||
                    lower.startsWith("during reading") ||
                    lower.startsWith("after reading") ||
                    lower.startsWith("extending learning")) {

                // 줄 단위 분리
                String[] lines = block.split("\n");

                if (lines.length > 0) {
                    // 첫 줄 = 헤더 강조
                    sb.append("<p><emphasis level=\"strong\">")
                            .append(lines[0].trim())
                            .append("</emphasis></p>");
                }

                // 나머지 줄 = 일반 문단
                for (int i = 1; i < lines.length; i++) {
                    if (!lines[i].trim().isEmpty()) {
                        sb.append("<p>").append(lines[i].trim()).append("</p>");
                    }
                }
            }
            // 일반 문장
            else {
                sb.append("<p>").append(block).append("</p>");
            }
        }

        sb.append("</speak>");
        return sb.toString();
    }
}