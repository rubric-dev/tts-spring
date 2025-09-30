package rubric_labs.tts_project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class HtmlPackager {

    public byte[] buildHtmlPackage(String title, List<byte[]> pageImages, List<byte[]> pageAudios) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // 1. index.html 생성
            String html = buildIndexHtml(title, pageImages.size());
            put(zos, "index.html", html.getBytes(StandardCharsets.UTF_8));

            // 2. 페이지 이미지들
            for (int i = 0; i < pageImages.size(); i++) {
                String imgPath = String.format("images/page-%d.png", i + 1);
                put(zos, imgPath, pageImages.get(i));
            }

            // 3. 페이지 오디오들
            for (int i = 0; i < pageAudios.size(); i++) {
                String audioPath = String.format("audio/page-%d.mp3", i + 1);
                put(zos, audioPath, pageAudios.get(i));
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("HTML 패키지 생성 실패: " + e.getMessage(), e);
        }
    }

    private String buildIndexHtml(String title, int pageCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>""").append(escapeHtml(title)).append("""
                </title>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                            padding: 20px;
                        }
                        
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                        }
                        
                        h1 {
                            text-align: center;
                            color: white;
                            font-size: 2.5rem;
                            margin-bottom: 40px;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
                        }
                        
                        .pages-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
                            gap: 30px;
                            padding: 20px;
                        }
                        
                        .page-card {
                            background: white;
                            border-radius: 15px;
                            overflow: hidden;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                            transition: transform 0.3s ease, box-shadow 0.3s ease;
                            cursor: pointer;
                            position: relative;
                        }
                        
                        .page-card:hover {
                            transform: translateY(-10px);
                            box-shadow: 0 15px 40px rgba(0,0,0,0.4);
                        }
                        
                        .page-card.playing {
                            transform: scale(1.05);
                            box-shadow: 0 0 30px rgba(102, 126, 234, 0.8);
                        }
                        
                        .page-image {
                            width: 100%;
                            height: auto;
                            display: block;
                        }
                        
                        .page-number {
                            position: absolute;
                            top: 10px;
                            right: 10px;
                            background: rgba(102, 126, 234, 0.9);
                            color: white;
                            padding: 8px 15px;
                            border-radius: 20px;
                            font-weight: bold;
                            font-size: 0.9rem;
                        }
                        
                        .play-icon {
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            transform: translate(-50%, -50%);
                            width: 80px;
                            height: 80px;
                            background: rgba(102, 126, 234, 0.9);
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            opacity: 0;
                            transition: opacity 0.3s ease;
                            pointer-events: none;
                        }
                        
                        .page-card:hover .play-icon {
                            opacity: 1;
                        }
                        
                        .play-icon::after {
                            content: '';
                            width: 0;
                            height: 0;
                            border-left: 25px solid white;
                            border-top: 15px solid transparent;
                            border-bottom: 15px solid transparent;
                            margin-left: 5px;
                        }
                        
                        .playing .play-icon::after {
                            border-left: none;
                            border-top: none;
                            border-bottom: none;
                            width: 8px;
                            height: 30px;
                            background: white;
                            box-shadow: 15px 0 0 white;
                            margin-left: 0;
                        }
                        
                        .info {
                            text-align: center;
                            color: white;
                            margin-top: 30px;
                            font-size: 0.9rem;
                            opacity: 0.8;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>""").append(escapeHtml(title)).append("""
                </h1>
                        <div class="pages-grid" id="pagesGrid">
                """);

        // 페이지 카드들 생성
        for (int i = 1; i <= pageCount; i++) {
            sb.append(String.format("""
                            <div class="page-card" data-page="%d" onclick="playPage(%d)">
                                <img class="page-image" src="images/page-%d.png" alt="Page %d">
                                <div class="page-number">Page %d</div>
                                <div class="play-icon"></div>
                            </div>
                    """, i, i, i, i, i));
        }

        sb.append("""
                        </div>
                        <div class="info">
                            클릭하여 각 페이지의 음성을 들어보세요 🔊
                        </div>
                    </div>
                    
                    <script>
                        let currentAudio = null;
                        let currentCard = null;
                        
                        function playPage(pageNum) {
                            // 이전 오디오 정지
                            if (currentAudio) {
                                currentAudio.pause();
                                currentAudio.currentTime = 0;
                            }
                            
                            // 이전 카드 스타일 제거
                            if (currentCard) {
                                currentCard.classList.remove('playing');
                            }
                            
                            // 같은 페이지를 다시 클릭하면 정지
                            const clickedCard = document.querySelector(`[data-page="${pageNum}"]`);
                            if (currentCard === clickedCard && currentAudio) {
                                currentAudio = null;
                                currentCard = null;
                                return;
                            }
                            
                            // 새 오디오 재생
                            currentAudio = new Audio(`audio/page-${pageNum}.mp3`);
                            currentCard = clickedCard;
                            currentCard.classList.add('playing');
                            
                            currentAudio.play();
                            
                            // 재생 완료 시 스타일 제거
                            currentAudio.onended = function() {
                                if (currentCard) {
                                    currentCard.classList.remove('playing');
                                }
                                currentAudio = null;
                                currentCard = null;
                            };
                        }
                    </script>
                </body>
                </html>
                """);

        return sb.toString();
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