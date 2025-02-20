import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot {
    public static void main(String[] args) {
        // 1. 필수 환경변수 가져오기 및 null 체크
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        if (webhookUrl == null || message == null || llmUrl == null || llmKey == null) {
            System.err.println("필수 환경변수가 설정되지 않았습니다.");
            return;
        }

        // 2. 사용할 모델 이름 지정
        String modelName = "mixtral-8x7b-32768";

        // 3. 원본 메시지를 한 번만 escape 처리하여 LLM 요청용 JSON 문자열 생성
        String escapedMessage = jsonEscape(message);
        String llmRequestBody = String.format(
            "{\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"model\":\"%s\"}",
            escapedMessage, modelName
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody))
            .build();

        try {
            // 4. LLM 서버에 POST 요청 보내고 응답 문자열 받기
            HttpResponse<String> llmResponse = client.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            String llmBody = llmResponse.body();

            // 5. 정규표현식으로 "content" 값을 추출  
            //    (응답 형식이 일정하다고 가정)
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"");
            Matcher matcher = pattern.matcher(llmBody);
            String content;
            if (matcher.find()) {
                content = matcher.group(1);
                // 응답 문자열에서 escape된 문자를 한 번 복원
                content = unescapeJson(content);
            } else {
                System.err.println("응답에서 'content' 값을 찾지 못했습니다.");
                return;
            }

            // 6. Slack 전송용 JSON 생성 시 unescape한 content를 한 번만 escape 처리
            String slackJson = String.format("{\"text\":\"%s\"}", jsonEscape(content));

            HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(slackJson))
                .build();

            // 7. Slack 웹훅으로 POST 요청 전송
            HttpResponse<String> slackResponse = client.send(slackRequest, HttpResponse.BodyHandlers.ofString());
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON 문자열에 사용할 때 필요한 이스케이프 처리를 한 번만 수행합니다.
     */
    private static String jsonEscape(String s) {
        if (s == null) return null;
        // 역슬래시를 먼저 처리하여 중복 이스케이프를 방지합니다.
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\b", "\\b");
        s = s.replace("\f", "\\f");
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "\\r");
        s = s.replace("\t", "\\t");
        return s;
    }

    /**
     * JSON 문자열 내 이스케이프된 문자를 원래 문자로 복원합니다.
     * (기본적인 escape 문자만 처리합니다.)
     */
    private static String unescapeJson(String s) {
        if (s == null) return null;
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        s = s.replace("\\b", "\b");
        s = s.replace("\\f", "\f");
        s = s.replace("\\n", "\n");
        s = s.replace("\\r", "\r");
        s = s.replace("\\t", "\t");
        return s;
    }
}
