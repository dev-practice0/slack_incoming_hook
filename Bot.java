import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

public class Bot {
    public static void main(String[] args) { // 진입부분
        // 이게 있어야 이 클래스를 실행했을 때 작동을 함
        // 웹훅을 만들 거임 -> URL 필요함
        // 환경변수로 받아올 것임 -> yml 파일에서 전달하게
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
                "{\"model\": \"meta-llama/Llama-3.3-70B-Instruct-Turbo\",\"messages\": [" + message + "],}"
            )).build();
        HttpResponse<String> llmResponse = null; // null 자리는 잡아줌
        try {
            // scope 문제
            // HttpResponse<String> llmResponse = llmClient.send(
            //     llmRequest, HttpResponse.BodyHandlers.ofString()
            // );
            llmResponse = llmClient.send(
                llmRequest, HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("요청 코드: " + llmResponse.statusCode());
            System.out.println("응답 결과: " + llmResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }        

        // Java 11 -> fetch
        HttpClient client = HttpClient.newHttpClient();
        // 요청을 얹힐 거다
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            // .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"테스트 메시지\"}")) 
            // .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\" + " + message + "\"}")) 
            .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\" + " + llmResponse.body() + "\"}")) 
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