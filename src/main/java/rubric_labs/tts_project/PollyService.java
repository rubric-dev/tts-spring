package rubric_labs.tts_project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollyService {

    private final PollyClient pollyClient;

    public byte[] synthesize(String ssml) {
        // 기본 음성으로 합성 (기존 호환성)
        return synthesizeWithVoice(ssml, "Joanna");
    }

    public byte[] synthesizeWithVoice(String ssml, String voiceId) {
        try {
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(ssml)
                    .textType(TextType.SSML)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.MP3)
                    .build();

            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            byte[] audioBytes = response.readAllBytes();

            log.info("음성 합성 완료: {} bytes, 음성: {}", audioBytes.length, voiceId);
            return audioBytes;

        } catch (PollyException | IOException e) {
            log.error("음성 합성 실패: 음성={}, 에러={}", voiceId, e.getMessage());
            throw new RuntimeException("음성 합성 실패: " + e.getMessage(), e);
        }
    }

    public ParagraphTts synthesizeParagraph(ParagraphSegment segment, String voiceId) {
        try {
            // 1) 음성 파일 생성
            byte[] mp3Data = synthesizeWithVoice(segment.getSsml(), voiceId);

            // 2) SpeechMarks 생성 (타이밍 정보)
            List<SpeechMark> speechMarks = getSpeechMarks(segment.getSsml(), voiceId);

            return new ParagraphTts(segment.getIndex(), mp3Data, speechMarks);

        } catch (Exception e) {
            log.error("문단 {} 음성 합성 실패: {}", segment.getIndex(), e.getMessage());
            throw new RuntimeException("문단 음성 합성 실패", e);
        }
    }

    private List<SpeechMark> getSpeechMarks(String ssml, String voiceId) {
        try {
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                    .text(ssml)
                    .textType(TextType.SSML)
                    .voiceId(voiceId)
                    .outputFormat(OutputFormat.JSON)
                    .speechMarkTypes(SpeechMarkType.WORD, SpeechMarkType.SENTENCE)
                    .build();

            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);
            String jsonMarks = new String(response.readAllBytes());

            return parseSpeechMarks(jsonMarks);

        } catch (Exception e) {
            log.warn("SpeechMarks 생성 실패 (음성: {}): {}", voiceId, e.getMessage());
            return List.of(); // 빈 리스트 반환으로 계속 진행
        }
    }

    private List<SpeechMark> parseSpeechMarks(String json) {
        // JSON 라인별 파싱 (각 라인이 하나의 SpeechMark JSON)
        return json.lines()
                .filter(line -> !line.trim().isEmpty())
                .map(this::parseJsonLine)
                .filter(mark -> mark != null)
                .toList();
    }

    private SpeechMark parseJsonLine(String jsonLine) {
        try {
            // 간단한 JSON 파싱 (Jackson 대신 수동 파싱)
            if (jsonLine.contains("\"type\":\"word\"") || jsonLine.contains("\"type\":\"sentence\"")) {
                String type = extractJsonValue(jsonLine, "type");
                int start = Integer.parseInt(extractJsonValue(jsonLine, "start"));
                int end = Integer.parseInt(extractJsonValue(jsonLine, "end"));
                long time = Long.parseLong(extractJsonValue(jsonLine, "time"));
                String value = extractJsonValue(jsonLine, "value");

                return new SpeechMark(type, start, end, time, value);
            }
        } catch (Exception e) {
            log.debug("SpeechMark 파싱 실패: {}", jsonLine);
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // 숫자 값인 경우
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start == -1) return "";
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        } else {
            start += pattern.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
    }

    // 음성 목록 조회 (디버깅/관리 용도)
    public List<String> getAvailableVoices() {
        try {
            DescribeVoicesResponse response = pollyClient.describeVoices();
            return response.voices().stream()
                    .filter(voice -> voice.supportedEngines().contains(Engine.STANDARD) ||
                            voice.supportedEngines().contains(Engine.NEURAL))
                    .map(voice -> voice.id().toString() + " (" + voice.languageCode() + ")")
                    .sorted()
                    .toList();
        } catch (Exception e) {
            log.error("음성 목록 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // 언어별 추천 음성 조회
    public String getRecommendedVoice(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "ko", "ko-kr" -> "Seoyeon";  // 한국어
            case "ja", "ja-jp" -> "Mizuki";   // 일본어
            case "zh", "zh-cn" -> "Zhiyu";    // 중국어 (간체)
            case "zh-tw" -> "Hiujin";         // 중국어 (번체)
            case "es", "es-es" -> "Lucia";    // 스페인어
            case "fr", "fr-fr" -> "Celine";   // 프랑스어
            case "de", "de-de" -> "Vicki";    // 독일어
            default -> "Joanna";              // 영어 (기본값)
        };
    }
}