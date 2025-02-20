import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class Bot {
    public static void main(String[] args) {
        // 환경변수를 통해 필요한 URL과 메시지 값을 가져옵니다.
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");
        
        // 사용할 모델 이름을 설정합니다.
        String modelName = "mixtral-8x7b-32768";
        // LLM 요청용 JSON 본문을 생성합니다.
        // 내부의 " 기호는 이스케이프 처리하여 JSON 형식을 유지합니다.
        String llmRequestBody = """
            {"messages":[{"role":"user","content":"%s"}],"model":"%s"}
            """.formatted(message.replace("\"", "\\\""), modelName);
            
        // LLM 서버와 통신하기 위한 HttpClient 객체를 생성합니다.
        HttpClient llmClient = HttpClient.newHttpClient();
        // LLM 서버에 보낼 POST 요청 객체를 구성합니다.
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody))
            .build();
            
        try {
            // LLM 서버에 POST 요청을 보내고 응답을 문자열로 받습니다.
            HttpResponse<String> llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            String llmBody = llmResponse.body();
            
            // 문자열 인덱싱을 사용하여 응답 JSON 내 "message" 객체의 "content" 값을 추출합니다.
            int messageStart = llmBody.indexOf("\"message\":{");
            int contentStart = llmBody.indexOf("\"content\":\"", messageStart) + 10;
            int contentEnd = llmBody.indexOf("\"},\"logprobs\"");
            String content = llmBody.substring(contentStart, contentEnd).replace("\\\"", "\"");
            
            // Slack으로 보낼 메시지 JSON을 생성합니다.
            String slackJson = """
                {"text":"%s"}
                """.formatted(content.replace("\"", "\\\""));
                
            // Slack 웹훅 호출을 위한 HttpClient 객체를 생성합니다.
            HttpClient slackClient = HttpClient.newHttpClient();
            // Slack 웹훅 URL에 POST 요청을 보내기 위한 요청 객체를 구성합니다.
            HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(slackJson))
                .build();
                
            // Slack 웹훅으로 POST 요청을 보내고 응답을 받습니다.
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            // 예외 발생 시 스택 트레이스를 출력합니다.
            e.printStackTrace();
        }
    }
}
