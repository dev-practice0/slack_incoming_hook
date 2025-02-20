import java.net.*;
import java.net.http.*;
import java.util.*;

public class Bot {
    public static void main(String[] args) {
        // 환경변수로부터 값 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");

        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        HttpClient llmClient = HttpClient.newHttpClient();
        // 메시지를 객체 형태로 변경: role과 content 지정
        String llmJson = "{\"model\": \"meta-llama/Llama-3.3-70B-Instruct-Turbo\", \"messages\": [{\"role\":\"user\", \"content\":\"" + message + "\"}]}";
        
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmJson))
            .build();

        HttpResponse<String> llmResponse = null;
        try {
            llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("요청 코드: " + llmResponse.statusCode());
            System.out.println("응답 결과: " + llmResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpClient client = HttpClient.newHttpClient();
        // LLM 응답을 Slack으로 전송할 때도 JSON 문자열 포맷을 맞춤
        String slackJson = "{\"text\":\"" + llmResponse.body() + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackJson))
            .build();
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("요청 코드: " + response.statusCode());
            System.out.println("응답 결과: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
