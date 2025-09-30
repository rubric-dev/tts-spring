package rubric_labs.tts_project;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3UploadService {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * EPUB 및 모든 리소스를 S3에 업로드하고 manifest.json URL 반환
     * 1. EPUB 압축 파일
     * 2. EPUB 언집 파일들 (XHTML, SMIL, OPF, 이미지, 오디오 등)
     * 3. manifest.json
     */
    public String uploadEpubWithResources(
            String bookId,
            BookMetadata metadata,
            byte[] epubBytes,
            List<ParagraphTts> ttsList,
            List<byte[]> pageImages,
            List<ParagraphSegment> segments
    ) throws IOException {

        String basePath = "books/" + bookId + "/";

        // 1. EPUB 압축 파일 업로드
        String epubKey = basePath + "book.epub";
        uploadToS3(epubKey, epubBytes, "application/epub+zip");
        log.info("✓ EPUB 압축 파일 업로드 완료: {}", epubKey);

        // 2. EPUB 언집하여 내부 파일들 업로드
        unzipAndUploadEpubContents(basePath + "epub/", epubBytes);
        log.info("✓ EPUB 언집 파일들 업로드 완료");

        // 3. 추가 오디오 파일들 업로드 (EPUB 외부용)
        List<String> audioUrls = new ArrayList<>();
        for (int i = 0; i < ttsList.size(); i++) {
            ParagraphTts tts = ttsList.get(i);
            String audioKey = basePath + "audio/page_" + (i + 1) + ".mp3";
            uploadToS3(audioKey, tts.getMp3(), "audio/mpeg");
            audioUrls.add(getS3Url(audioKey));
        }
        log.info("✓ 오디오 파일 {} 개 업로드 완료", audioUrls.size());

        // 4. 추가 이미지 파일들 업로드 (EPUB 외부용)
        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < pageImages.size(); i++) {
            String imageKey = basePath + "images/page_" + (i + 1) + ".jpg";
            uploadToS3(imageKey, pageImages.get(i), "image/jpeg");
            imageUrls.add(getS3Url(imageKey));
        }
        log.info("✓ 이미지 파일 {} 개 업로드 완료", imageUrls.size());

        // 5. manifest.json 생성 및 업로드
        ManifestJson manifest = createManifest(
                bookId,
                metadata,
                getS3Url(epubKey),
                audioUrls,
                imageUrls,
                segments
        );

        String manifestKey = basePath + "manifest.json";
        String manifestJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(manifest);

        uploadToS3(manifestKey, manifestJson.getBytes(StandardCharsets.UTF_8), "application/json");
        log.info("✓ manifest.json 업로드 완료: {}", manifestKey);

        String manifestUrl = getS3Url(manifestKey);
        log.info("✅ 모든 파일 업로드 완료. Manifest URL: {}", manifestUrl);

        return manifestUrl;
    }

    /**
     * EPUB 파일을 언집하여 내부 파일들을 S3에 업로드
     */
    private void unzipAndUploadEpubContents(String basePath, byte[] epubBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(epubBytes);
             ZipInputStream zis = new ZipInputStream(bais, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            int fileCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String fileName = entry.getName();

                // 파일 내용 읽기
                byte[] fileData = readZipEntryData(zis);

                // Content-Type 추론
                String contentType = getContentType(fileName);

                // S3에 업로드
                String s3Key = basePath + fileName;
                uploadToS3(s3Key, fileData, contentType);

                fileCount++;
                log.debug("  - {} 업로드 완료 ({})", fileName, contentType);

                zis.closeEntry();
            }

            log.info("✓ EPUB 내부 파일 {} 개 언집 및 업로드 완료", fileCount);
        }
    }

    /**
     * ZipEntry에서 데이터 읽기
     */
    private byte[] readZipEntryData(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }

    /**
     * 파일 확장자로 Content-Type 추론
     */
    private String getContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.endsWith(".xhtml") || lowerFileName.endsWith(".html")) {
            return "application/xhtml+xml";
        } else if (lowerFileName.endsWith(".xml")) {
            return "application/xml";
        } else if (lowerFileName.endsWith(".smil")) {
            return "application/smil+xml";
        } else if (lowerFileName.endsWith(".opf")) {
            return "application/oebps-package+xml";
        } else if (lowerFileName.endsWith(".ncx")) {
            return "application/x-dtbncx+xml";
        } else if (lowerFileName.endsWith(".css")) {
            return "text/css";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerFileName.endsWith(".m4a")) {
            return "audio/mp4";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * S3에 파일 업로드
     */
    private void uploadToS3(String key, byte[] data, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
    }

    /**
     * S3 URL 생성
     */
    private String getS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, key);
    }

    /**
     * manifest.json 생성
     */
    private ManifestJson createManifest(
            String bookId,
            BookMetadata metadata,
            String epubUrl,
            List<String> audioUrls,
            List<String> imageUrls,
            List<ParagraphSegment> segments
    ) {
        ManifestJson manifest = new ManifestJson();
        manifest.setBookId(bookId);
        manifest.setTitle(metadata.getTitle());
        manifest.setAuthor(metadata.getAuthor());
        manifest.setLanguage(metadata.getLanguage());
        manifest.setEpubUrl(epubUrl);
        manifest.setCreatedAt(new Date());

        // 페이지 정보 생성
        List<ManifestPage> pages = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            ManifestPage page = new ManifestPage();
            page.setPageNumber(i + 1);
            page.setImageUrl(imageUrls.get(i));
            page.setAudioUrl(i < audioUrls.size() ? audioUrls.get(i) : null);

            // 해당 페이지의 텍스트 추가
            if (i < segments.size()) {
                ParagraphSegment segment = segments.get(i);
                page.setText(String.join(" ", segment.sentences()));
            }

            pages.add(page);
        }

        manifest.setPages(pages);
        manifest.setTotalPages(pages.size());

        return manifest;
    }

    // 기존 메서드 (하위 호환성)
    public void uploadEpubToS3(byte[] epubBytes, String bucketName, String key) throws IOException {
        uploadToS3(key, epubBytes, "application/epub+zip");
    }
}