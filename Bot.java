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

        // 3. JSON 문자열 구성 시 헬퍼 함수를 사용하여 한 번만 이스케이프 처리합니다.
        String llmRequestBody = String.format(
            "{\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"model\":\"%s\"}",
            jsonEscape(message), modelName
        );

        // 4. LLM 서버에 요청할 HttpClient 및 요청 객체 생성
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody))
            .build();

        try {
            // 5. LLM 서버에 POST 요청 보내고 응답 문자열 받기
            HttpResponse<String> llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            String llmBody = llmResponse.body();

            // 6. 정규표현식으로 "message" 객체 내부의 "content" 값을 추출  
            //    DOTALL 옵션을 사용해 여러 줄에 걸친 내용도 매칭합니다.
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\\{.*?\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(llmBody);
            String content;
            if (matcher.find()) {
                content = matcher.group(1);
                // 응답 JSON에서 이스케이프된 문자를 복원 (한 번만 unescape)
                content = unescapeJson(content);
            } else {
                System.err.println("응답에서 'content' 값을 찾지 못했습니다.");
                return;
            }

            // 7. Slack에 보낼 메시지 JSON 구성 시도 역시 헬퍼 함수를 통해 이스케이프 처리 (한 번만)
            String slackJson = String.format("{\"text\":\"%s\"}", jsonEscape(content));

            // 8. Slack 웹훅 호출을 위한 HttpClient 및 요청 객체 생성
            HttpClient slackClient = HttpClient.newHttpClient();
            HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(slackJson))
                .build();

            // 9. Slack 웹훅으로 POST 요청 전송
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
            // 필요시 slackResponse.statusCode()나 응답 내용을 확인할 수 있습니다.
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON 문자열로 사용할 때 필요한 이스케이프 처리를 한 번만 수행하는 함수입니다.
     * 주의: 모든 경우를 완벽하게 처리하지는 않으므로, 복잡한 문자열에는 추가 검증이 필요할 수 있습니다.
     */
    private static String jsonEscape(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * JSON 문자열 내 이스케이프된 문자를 원래 문자로 복원하는 단순 unescape 함수입니다.
     * 단, 복잡한 케이스는 고려하지 않으므로 기본적인 이스케이프 문자만 처리합니다.
     */
    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
