package rubric_labs.tts_project;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/tts")
@RestController
public class TteApi {
    private final PollyService pollyService;
    private final PdfReader pdfReader;
    private final PdfMetadataExtractor metadataExtractor;

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
}