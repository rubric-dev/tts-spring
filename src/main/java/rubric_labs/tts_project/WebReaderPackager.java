package rubric_labs.tts_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WebReaderPackager {

    public byte[] buildWebReaderPackage(String title, byte[] epubBytes, List<byte[]> pageImages) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            String html = buildReaderHtml(title);
            put(zos, "index.html", html.getBytes(StandardCharsets.UTF_8));
            put(zos, "book.epub", epubBytes);

            if (pageImages != null && !pageImages.isEmpty()) {
                put(zos, "thumbnail.png", pageImages.get(0));
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("웹 리더 패키지 생성 실패: " + e.getMessage(), e);
        }
    }

    private String buildReaderHtml(String title) {
        String escapedTitle = escapeHtml(title);
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + escapedTitle + "</title>\n" +
                "    <link rel=\"icon\" href=\"data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📚</text></svg>\">\n" +
// ... (CSS 부분은 동일) ...
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
// ... (HTML 구조는 동일) ...
                "    </div>\n" +
// 여기부터 수정!
                "    <script src=\"https://cdn.jsdelivr.net/npm/jszip@3/dist/jszip.min.js\"></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/epubjs@0.3/dist/epub.min.js\"></script>\n" +
                "    <script>\n" +
                "        let book;\n" +
                "        let rendition;\n" +
                "        let isPlaying = false;\n" +
                "        \n" +
                "        async function initReader() {\n" +
                "            try {\n" +
                "                disableButtons();\n" +
                "                const viewerElement = document.getElementById('viewer');\n" +
                "                viewerElement.innerHTML = '<div class=\"loading\">📖 책을 불러오는 중...</div>';\n" +
                "                \n" +
                "                console.log('EPUB 로드 시작...');\n" +
                "                \n" +
                "                // EPUB 로드 (절대 경로 사용)\n" +
                "                const epubUrl = window.location.origin + window.location.pathname.replace('index.html', '') + 'book.epub';\n" +
                "                console.log('EPUB URL:', epubUrl);\n" +
                "                \n" +
                "                book = ePub(epubUrl);\n" +
                "                \n" +
                "                console.log('Book 객체 생성 완료');\n" +
                "                \n" +
                "                // 렌더링\n" +
                "                rendition = book.renderTo('viewer', {\n" +
                "                    width: '100%',\n" +
                "                    height: '100%',\n" +
                "                    spread: 'none',\n" +
                "                    flow: 'paginated'\n" +
                "                });\n" +
                "                \n" +
                "                console.log('Rendition 생성 완료');\n" +
                "                \n" +
                "                // 메타데이터 로드 대기\n" +
                "                await book.ready;\n" +
                "                console.log('Book ready');\n" +
                "                \n" +
                "                // 첫 페이지 표시\n" +
                "                await rendition.display();\n" +
                "                console.log('첫 페이지 표시 완료');\n" +
                "                \n" +
                "                enableButtons();\n" +
                "                \n" +
                "                book.loaded.metadata.then(function(metadata) {\n" +
                "                    console.log('책 제목:', metadata.title);\n" +
                "                    console.log('저자:', metadata.creator);\n" +
                "                });\n" +
                "                \n" +
                "                // 키보드 네비게이션\n" +
                "                document.addEventListener('keydown', function(e) {\n" +
                "                    if (e.key === 'ArrowRight') nextPage();\n" +
                "                    else if (e.key === 'ArrowLeft') prevPage();\n" +
                "                });\n" +
                "                \n" +
                "                console.log('✅ EPUB 리더 초기화 완료');\n" +
                "            } catch (error) {\n" +
                "                console.error('❌ EPUB 로딩 실패:', error);\n" +
                "                console.error('Error stack:', error.stack);\n" +
                "                document.getElementById('viewer').innerHTML = \n" +
                "                    '<div class=\"error\">❌ 책을 불러올 수 없습니다.<br><br>' + error.message + '<br><br>콘솔을 확인하세요.</div>';\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function disableButtons() {\n" +
                "            document.getElementById('prev').disabled = true;\n" +
                "            document.getElementById('next').disabled = true;\n" +
                "            document.getElementById('play').disabled = true;\n" +
                "        }\n" +
                "        \n" +
                "        function enableButtons() {\n" +
                "            document.getElementById('prev').disabled = false;\n" +
                "            document.getElementById('next').disabled = false;\n" +
                "            document.getElementById('play').disabled = false;\n" +
                "        }\n" +
                "        \n" +
                "        function prevPage() {\n" +
                "            if (rendition) rendition.prev();\n" +
                "        }\n" +
                "        \n" +
                "        function nextPage() {\n" +
                "            if (rendition) rendition.next();\n" +
                "        }\n" +
                "        \n" +
                "        function togglePlay() {\n" +
                "            if (!rendition) return;\n" +
                "            const playBtn = document.getElementById('play');\n" +
                "            if (!isPlaying) {\n" +
                "                playBtn.textContent = '⏸ 정지';\n" +
                "                isPlaying = true;\n" +
                "            } else {\n" +
                "                playBtn.textContent = '▶ 재생';\n" +
                "                isPlaying = false;\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function openFullscreen() {\n" +
                "            const elem = document.querySelector('.reader-container');\n" +
                "            if (elem.requestFullscreen) elem.requestFullscreen();\n" +
                "            else if (elem.webkitRequestFullscreen) elem.webkitRequestFullscreen();\n" +
                "        }\n" +
                "        \n" +
                "        function downloadEpub() {\n" +
                "            const link = document.createElement('a');\n" +
                "            link.href = 'book.epub';\n" +
                "            link.download = '" + escapedTitle + ".epub';\n" +
                "            link.click();\n" +
                "        }\n" +
                "        \n" +
                "        window.addEventListener('load', initReader);\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private void put(ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}