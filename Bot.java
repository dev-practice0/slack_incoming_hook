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

        // 2. 사용할 모델 이름 설정
        String modelName = "mixtral-8x7b-32768";

        // 3. LLM 요청용 JSON 문자열 생성  
        //    텍스트 블록을 사용하며, 내부의 " 기호는 이스케이프 처리합니다.
        String llmRequestBody = """
            {"messages":[{"role":"user","content":"%s"}],"model":"%s"}
            """.formatted(message.replace("\"", "\\\""), modelName);

        // 4. HttpClient 생성 및 LLM 요청 객체 구성
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

            // 6. 정규표현식을 사용하여 "message" 객체 내의 "content" 값을 추출  
            //    DOTALL 옵션으로 여러 줄에 걸친 내용도 매칭합니다.
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\\{.*?\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(llmBody);

            String content = null;
            if (matcher.find()) {
                content = matcher.group(1);
                // 이스케이프된 따옴표를 실제 따옴표로 복원
                content = content.replace("\\\"", "\"");
            } else {
                System.err.println("응답에서 'content' 값을 찾을 수 없습니다.");
                return;
            }

            // 7. Slack에 보낼 메시지 JSON 생성 (내부의 " 기호 이스케이프 처리)
            String slackJson = """
                {"text":"%s"}
                """.formatted(content.replace("\"", "\\\""));

            // 8. Slack 웹훅 호출을 위한 HttpClient 및 요청 객체 구성
            HttpClient slackClient = HttpClient.newHttpClient();
            HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(slackJson))
                .build();

            // 9. Slack 웹훅으로 POST 요청 전송
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
            // 필요시 slackResponse.statusCode()나 본문을 확인할 수 있습니다.
        } catch (IOException | InterruptedException e) {
            // 네트워크 문제나 요청 중 인터럽트 발생 시 예외 출력
            e.printStackTrace();
        }
    }
}
