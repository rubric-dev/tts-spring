package rubric_labs.tts_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EpubPackager {

    /**
     * ✅ 페이지별 XHTML/SMIL 파일로 EPUB 생성
     */
    public byte[] buildEpubWithPages(
            List<XhtmlBuilder.PageXhtml> pageXhtmls,
            List<SmilBuilder.PageSmil> pageSmils,
            String opf,
            String containerXml,
            String nav,
            List<ParagraphTts> ttsList,
            List<byte[]> pageImages
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // 1) mimetype (반드시 첫 엔트리, STORED)
            addStoredEntry(zos, "mimetype", "application/epub+zip".getBytes(StandardCharsets.US_ASCII));

            // 2) META-INF/container.xml
            put(zos, "META-INF/container.xml", containerXml.getBytes(StandardCharsets.UTF_8));

            // 3) ✅ 페이지별 XHTML 파일들
            for (XhtmlBuilder.PageXhtml pageXhtml : pageXhtmls) {
                String path = "OEBPS/text/" + pageXhtml.getFileName();
                put(zos, path, pageXhtml.getContent().getBytes(StandardCharsets.UTF_8));
            }

            // 3.5) OEBPS/nav.xhtml
            put(zos, "OEBPS/nav.xhtml", nav.getBytes(StandardCharsets.UTF_8));

            // 4) ✅ 페이지별 SMIL 파일들
            for (SmilBuilder.PageSmil pageSmil : pageSmils) {
                String path = "OEBPS/smil/" + pageSmil.getFileName();
                put(zos, path, pageSmil.getContent().getBytes(StandardCharsets.UTF_8));
            }

            // 5) OEBPS/audio/*.mp3
            for (ParagraphTts t : ttsList) {
                String name = "OEBPS/audio/chap1_p" + t.getParagraphIndex() + ".mp3";
                put(zos, name, t.getMp3());
            }

            // 5.5) OEBPS/images/page-<index>.png
            if (pageImages != null) {
                for (int i = 0; i < pageImages.size(); i++) {
                    String imgPath = String.format("OEBPS/images/page-%d.png", i + 1);
                    put(zos, imgPath, pageImages.get(i));
                }
            }

            // 6) OEBPS/content.opf
            put(zos, "OEBPS/content.opf", opf.getBytes(StandardCharsets.UTF_8));

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 기존 메서드 (하위 호환성 유지)
     * @deprecated buildEpubWithPages() 사용 권장
     */
    @Deprecated
    public byte[] buildEpub(String xhtml, String smil, String opf, String containerXml,
                            String nav,
                            List<ParagraphTts> ttsList, List<byte[]> pageImages) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // 1) mimetype (반드시 첫 엔트리, STORED)
            addStoredEntry(zos, "mimetype", "application/epub+zip".getBytes(StandardCharsets.US_ASCII));

            // 2) META-INF/container.xml
            put(zos, "META-INF/container.xml", containerXml.getBytes(StandardCharsets.UTF_8));

            // 3) OEBPS/text/chap1.xhtml
            put(zos, "OEBPS/text/chap1.xhtml", xhtml.getBytes(StandardCharsets.UTF_8));

            // 3.5) OEBPS/nav.xhtml
            put(zos, "OEBPS/nav.xhtml", nav.getBytes(StandardCharsets.UTF_8));

            // 4) OEBPS/smil/chap1.smil
            put(zos, "OEBPS/smil/chap1.smil", smil.getBytes(StandardCharsets.UTF_8));

            // 5) OEBPS/audio/*.mp3
            for (ParagraphTts t : ttsList) {
                String name = "OEBPS/audio/chap1_p" + t.getParagraphIndex() + ".mp3";
                put(zos, name, t.getMp3());
            }

            // 5.5) OEBPS/images/page-<index>.png
            if (pageImages != null) {
                for (int i = 0; i < pageImages.size(); i++) {
                    String imgPath = String.format("OEBPS/images/page-%d.png", i + 1);
                    put(zos, imgPath, pageImages.get(i));
                }
            }

            // 6) OEBPS/content.opf
            put(zos, "OEBPS/content.opf", opf.getBytes(StandardCharsets.UTF_8));

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void put(ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private void addStoredEntry(ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }
}