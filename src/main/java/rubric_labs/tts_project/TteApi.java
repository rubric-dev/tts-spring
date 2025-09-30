package rubric_labs.tts_project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tts")
@RestController
public class TteApi {
    private final PollyService pollyService;
    private final PdfReader pdfReader;
    private final PdfMetadataExtractor metadataExtractor;
    private final S3UploadService s3UploadService;

    @PostMapping(value = "", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<byte[]> readPdf(@RequestPart("file") MultipartFile file) {
        String ssml = pdfReader.getSsmlFromPdfFile(file);
        byte[] mp3 = pollyService.synthesize(ssml);

        // 파일명을 원본 PDF 이름 기반으로 생성
        String originalName = file.getOriginalFilename();
        String outputName = (originalName != null ?
                originalName.replaceAll("\\.[^.]*$", "") : "output") + ".mp3";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + outputName + "\"")
                .body(mp3);
    }

    @PostMapping(value = "/epub", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<byte[]> makeEpub(@RequestPart("file") MultipartFile file) {
        // 1) PDF에서 메타데이터 자동 추출
        BookMetadata metadata = metadataExtractor.extractMetadata(file);

        // 2) 문단/문장 추출
        List<ParagraphSegment> segments = pdfReader.extractParagraphs(file);

        // 텍스트 + 오디오 외에 이미지 추출
        List<byte[]> pageImages = pdfReader.extractPageImages(file);
        int imageCount = pageImages != null ? pageImages.size() : 0;

        // 3) 메타데이터의 음성으로 문단별 Polly 호출
        List<ParagraphTts> ttsList = new ArrayList<>();
        for (ParagraphSegment seg : segments) {
            ttsList.add(pollyService.synthesizeParagraph(seg, metadata.getVoice()));
        }

        // 4) XHTML / SMIL / OPF / container.xml 생성 (메타데이터 활용)
        XhtmlBuilder xhtmlBuilder = new XhtmlBuilder();
        String xhtml = xhtmlBuilder.buildChapterXhtml(metadata.getTitle(), segments, imageCount);

        SmilBuilder smilBuilder = new SmilBuilder();
        String smil = smilBuilder.buildChapterSmil("text/chap1.xhtml", segments, ttsList, "audio");

        OpfBuilder opfBuilder = new OpfBuilder();
        String opf = opfBuilder.buildOpf(
                metadata.getBookId(),
                metadata.getTitle(),
                metadata.getAuthor(),
                metadata.getLanguage(),
                segments.size(),
                imageCount
        );
        String containerXml = opfBuilder.buildContainerXml();

        // 5) EPUB 패키징
        EpubPackager packager = new EpubPackager();
        NavBuilder navBuilder = new NavBuilder();
        String nav = navBuilder.buildNav(metadata.getTitle(), imageCount);

        byte[] epubBytes = packager.buildEpub(xhtml, smil, opf, containerXml, nav, ttsList, pageImages);

        // 출력 파일명을 책 제목 기반으로 생성
        String epubFilename = sanitizeFilename(metadata.getTitle()) + ".epub";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/epub+zip"))
                .header("Content-Disposition", "attachment; filename=\"" + epubFilename + "\"")
                .body(epubBytes);
    }

    private String sanitizeFilename(String filename) {
        // 파일명에 사용할 수 없는 문자 제거
        return filename.replaceAll("[^a-zA-Z0-9가-힣\\s\\-_]", "")
                .replaceAll("\\s+", "_")
                .substring(0, Math.min(filename.length(), 50)); // 최대 50자
    }

    @PostMapping(value = "/epub/v2", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<byte[]> makeEpubV2(@RequestPart("file") MultipartFile file) {
        // 1. 메타데이터 추출
        BookMetadata metadata = metadataExtractor.extractMetadata(file);

        // 2. 페이지별 이미지 추출
        List<byte[]> pageImages = pdfReader.extractPageImages(file);

        // 3. 페이지별 텍스트 추출
        List<String> pageTexts = pdfReader.extractTextByPage(file);

        // 4. 페이지별 음성 생성
        List<ParagraphTts> ttsList = new ArrayList<>();
        List<ParagraphSegment> segments = new ArrayList<>();

        for (int i = 0; i < pageImages.size(); i++) {
            int pageNumber = i + 1;

            // 텍스트를 SSML로 변환
            String pageText = i < pageTexts.size() ? pageTexts.get(i) : "";
            String ssml = pdfReader.convertToSSML(pageText, metadata);

            // 문장 분리 (SMIL 생성용)
            List<String> sentences = pdfReader.splitSentences(
                    pdfReader.sanitize(pageText, metadata.getLanguage()),
                    metadata.getLanguage()
            );

            // ParagraphSegment 생성
            ParagraphSegment segment = new ParagraphSegment(pageNumber, sentences, ssml);
            segments.add(segment);

            // 음성 합성
            ParagraphTts tts = pollyService.synthesizeParagraph(segment, metadata.getVoice());
            ttsList.add(tts);

            log.info("페이지 {}/{} 처리 완료", pageNumber, pageImages.size());
        }

        // 5. XHTML / SMIL / OPF 생성
        XhtmlBuilder xhtmlBuilder = new XhtmlBuilder();
        String xhtml = xhtmlBuilder.buildChapterXhtml(metadata.getTitle(), segments, pageImages.size());

        SmilBuilder smilBuilder = new SmilBuilder();
        String smil = smilBuilder.buildChapterSmil("text/chap1.xhtml", segments, ttsList, "audio");

        OpfBuilder opfBuilder = new OpfBuilder();
        String opf = opfBuilder.buildOpf(
                metadata.getBookId(),
                metadata.getTitle(),
                metadata.getAuthor(),
                metadata.getLanguage(),
                segments.size(),
                pageImages.size()
        );
        String containerXml = opfBuilder.buildContainerXml();

        // 6. EPUB 패키징
        EpubPackager packager = new EpubPackager();
        NavBuilder navBuilder = new NavBuilder();
        String nav = navBuilder.buildNav(metadata.getTitle(), pageImages.size());

        byte[] epubBytes = packager.buildEpub(xhtml, smil, opf, containerXml, nav, ttsList, pageImages);

        // 출력 파일명 생성
        String epubFilename = sanitizeFilename(metadata.getTitle()) + "_v2.epub";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/epub+zip"))
                .header("Content-Disposition", "attachment; filename=\"" + epubFilename + "\"")
                .body(epubBytes);
    }

    @PostMapping(value = "/epub/v2/s3", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ManifestResponse> makeEpubV2WithS3(@RequestPart("file") MultipartFile file) {
        try {
            // 1. 메타데이터 추출
            BookMetadata metadata = metadataExtractor.extractMetadata(file);
            String bookId = metadata.getBookId();

            // 2. 페이지별 이미지 추출
            List<byte[]> pageImages = pdfReader.extractPageImages(file);

            // 3. 페이지별 텍스트 추출
            List<String> pageTexts = pdfReader.extractTextByPage(file);

            // 4. 페이지별 음성 생성
            List<ParagraphTts> ttsList = new ArrayList<>();
            List<ParagraphSegment> segments = new ArrayList<>();

            for (int i = 0; i < pageImages.size(); i++) {
                int pageNumber = i + 1;

                // 텍스트를 SSML로 변환
                String pageText = i < pageTexts.size() ? pageTexts.get(i) : "";
                String ssml = pdfReader.convertToSSML(pageText, metadata);

                // 문장 분리 (SMIL 생성용)
                List<String> sentences = pdfReader.splitSentences(
                        pdfReader.sanitize(pageText, metadata.getLanguage()),
                        metadata.getLanguage()
                );

                // ParagraphSegment 생성
                ParagraphSegment segment = new ParagraphSegment(pageNumber, sentences, ssml);
                segments.add(segment);

                // 음성 합성
                ParagraphTts tts = pollyService.synthesizeParagraph(segment, metadata.getVoice());
                ttsList.add(tts);

                log.info("페이지 {}/{} 처리 완료", pageNumber, pageImages.size());
            }

            // 5. XHTML / SMIL / OPF 생성
            XhtmlBuilder xhtmlBuilder = new XhtmlBuilder();
            String xhtml = xhtmlBuilder.buildChapterXhtml(metadata.getTitle(), segments, pageImages.size());

            SmilBuilder smilBuilder = new SmilBuilder();
            String smil = smilBuilder.buildChapterSmil("text/chap1.xhtml", segments, ttsList, "audio");

            OpfBuilder opfBuilder = new OpfBuilder();
            String opf = opfBuilder.buildOpf(
                    metadata.getBookId(),
                    metadata.getTitle(),
                    metadata.getAuthor(),
                    metadata.getLanguage(),
                    segments.size(),
                    pageImages.size()
            );
            String containerXml = opfBuilder.buildContainerXml();

            // 6. EPUB 패키징
            EpubPackager packager = new EpubPackager();
            NavBuilder navBuilder = new NavBuilder();
            String nav = navBuilder.buildNav(metadata.getTitle(), pageImages.size());

            byte[] epubBytes = packager.buildEpub(xhtml, smil, opf, containerXml, nav, ttsList, pageImages);

            // 7. S3에 업로드 (bookId 기반 경로)
            String manifestUrl = s3UploadService.uploadEpubWithResources(
                    bookId,
                    metadata,
                    epubBytes,
                    ttsList,
                    pageImages,
                    segments
            );

            // 8. manifest.json URL 반환
            return ResponseEntity.ok(new ManifestResponse(manifestUrl, bookId));

        } catch (Exception e) {
            log.error("EPUB V2 S3 업로드 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    // Response DTO
    @Data
    @AllArgsConstructor
    public static class ManifestResponse {
        private String manifestUrl;
        private String bookId;
    }
}