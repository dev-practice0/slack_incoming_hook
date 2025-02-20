import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

public class Bot {
    public static void main(String[] args) { // 진입부분
        // 웹훅 URL과 메시지는 환경변수로 받음
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");

        // LLM 파트
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"model\": \"meta-llama/Llama-3.3-70B-Instruct-Turbo\",\"messages\": [" + message + "]}"
            ))
            .build();
        HttpResponse<String> llmResponse = null;
        try {
            llmResponse = llmClient.send(
                llmRequest, HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("요청 코드: " + llmResponse.statusCode());
            System.out.println("응답 결과: " + llmResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }        

        // Slack 웹훅 호출 (Java 11 HttpClient 사용)
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"text\":\"" + llmResponse.body() + "\"}"
            ))
            .build();
        
        try {
            HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("요청 코드: " + response.statusCode());
            System.out.println("응답 결과: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
