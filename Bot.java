import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class Bot {
    public static void main(String[] args) {
        // * 1. 환경변수에서 필요한 값을 읽어옵니다.
        //    - SLACK_WEBHOOK_URL: Slack 웹훅 URL
        //    - SLACK_WEBHOOK_MSG: 사용자 메시지 (LLM 요청의 내용)
        //    - LLM_URL: LLM API 엔드포인트
        //    - LLM_KEY: LLM API 인증키
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");
        
        // * 2. javax.json을 사용하여 LLM 요청 JSON을 생성합니다.
        //    - JavaScript의 JSON.stringify와 비슷하게 객체를 구성합니다.
        //    - "messages" 배열 내부에 사용자 메시지를 객체 형태({ "role": "user", "content": message })로 추가합니다.
        JsonObjectBuilder userMessageBuilder = Json.createObjectBuilder()
            .add("role", "user")
            .add("content", message);
        JsonArrayBuilder messagesArray = Json.createArrayBuilder()
            .add(userMessageBuilder);
        JsonObject llmRequestJson = Json.createObjectBuilder()
            .add("model", "meta-llama/Llama-3.3-70B-Instruct-Turbo")
            .add("messages", messagesArray)
            .build();
        
        // * 3. HttpClient를 사용하여 LLM API에 POST 요청을 전송합니다.
        //    - JavaScript의 fetch와 유사하게 동작하며, 헤더(Content-Type, Authorization) 설정 후 요청 본문에 JSON 문자열을 전송합니다.
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestJson.toString()))
            .build();
        
        HttpResponse<String> llmResponse = null;
        try {
            // * LLM API에 요청을 보내고 응답을 문자열로 받습니다.
            llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("LLM 요청 코드: " + llmResponse.statusCode());
            System.out.println("LLM 응답 결과: " + llmResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // * 4. LLM 응답 JSON을 파싱하여 choices 배열 내 첫 번째 요소의 message.content 값을 추출합니다.
        //    - JavaScript의 JSON.parse와 유사하게 javax.json.JsonReader를 사용합니다.
        String content = "";
        try (JsonReader jsonReader = Json.createReader(new StringReader(llmResponse.body()))) {
            JsonObject jsonLLM = jsonReader.readObject();
            content = jsonLLM.getJsonArray("choices")
                        .getJsonObject(0)
                        .getJsonObject("message")
                        .getString("content");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // * 5. Slack에 보낼 JSON 메시지를 javax.json을 사용해 생성합니다.
        //    - JavaScript에서 JSON.stringify로 객체를 문자열로 변환하는 것과 같은 역할을 합니다.
        //    - "text" 필드에 LLM 응답에서 추출한 content 값을 주입합니다.
        JsonObject slackJson = Json.createObjectBuilder()
            .add("text", content)
            .build();
        
        // * 6. Slack 웹훅 URL로 POST 요청을 보내어 메시지를 전달합니다.
        //    - fetch()를 사용하는 JavaScript와 유사하게 HttpClient를 사용합니다.
        HttpClient slackClient = HttpClient.newHttpClient();
        HttpRequest slackRequest = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackJson.toString()))
            .build();
        
        try {
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Slack 요청 코드: " + slackResponse.statusCode());
            System.out.println("Slack 응답 결과: " + slackResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
