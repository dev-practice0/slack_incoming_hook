import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class Bot {
    public static void main(String[] args) {
        // * 1. 환경변수에서 필요한 값들을 읽어옵니다.
        //    - SLACK_WEBHOOK_URL: Slack 웹훅 URL
        //    - SLACK_WEBHOOK_MSG: LLM 요청에 사용할 사용자 메시지
        //    - LLM_URL: LLM API 엔드포인트 URL
        //    - LLM_KEY: LLM API 인증 키
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");
        
        // * 2. LLM 요청 JSON 문자열을 직접 구성합니다.
        //    - 여기서는 messages 배열에 사용자 메시지를 객체로 포함시킵니다.
        //    - JSON 형식에 맞게 문자열 이스케이프 처리를 위해 escapeJson() 함수를 사용합니다.
        String llmRequestJson = "{\"model\": \"meta-llama/Llama-3.3-70B-Instruct-Turbo\", \"messages\": [{\"role\":\"user\", \"content\":\"" 
                                  + escapeJson(message) + "\"}]}";
        
        // * 3. Java 11 HttpClient를 사용하여 LLM API에 POST 요청을 보냅니다.
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestJson))
            .build();
        
        HttpResponse<String> llmResponse = null;
        try {
            // * LLM API에 요청을 보내고 응답을 문자열로 받습니다.
            llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("LLM 요청 코드: " + llmResponse.statusCode());
            System.out.println("LLM 응답 결과: " + llmResponse.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        
        // * 4. LLM 응답에서 choices[0].message.content 값을 단순 문자열 탐색으로 추출합니다.
        String llmBody = llmResponse.body();
        String content = extractContentFromLLMResponse(llmBody);
        
        // * 5. Slack 웹훅으로 전송할 JSON 문자열을 구성합니다.
        //    - content 값을 JSON 형식에 맞게 이스케이프 처리합니다.
        String slackJson = "{\"text\":\"" + escapeJson(content) + "\"}";
        
        // * 6. Slack 웹훅 URL로 POST 요청을 보내어 메시지를 전송합니다.
        HttpClient slackClient = HttpClient.newHttpClient();
        HttpRequest slackRequest = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackJson))
            .build();
        
        try {
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Slack 요청 코드: " + slackResponse.statusCode());
            System.out.println("Slack 응답 결과: " + slackResponse.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // * 간단한 JSON 문자열 이스케이프 함수
    //    - 백슬래시, 따옴표, 개행문자를 이스케이프합니다.
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n");
    }
    
    // * LLM 응답 JSON에서 choices[0].message.content 값을 추출하는 간단한 함수
    //    - 이 방식은 매우 단순하며, 실제 JSON 파싱보다는 취급할 수 있는 경우에 한정됩니다.
    private static String extractContentFromLLMResponse(String json) {
        // * "content": 문자열을 기준으로 값을 추출합니다.
        String target = "\"content\":";
        int idx = json.indexOf(target);
        if (idx == -1) {
            return "";
        }
        // * 첫 번째 따옴표 위치 찾기 (값의 시작)
        int startQuote = json.indexOf("\"", idx + target.length());
        if (startQuote == -1) {
            return "";
        }
        // * 두 번째 따옴표 위치 찾기 (값의 끝)
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) {
            return "";
        }
        return json.substring(startQuote + 1, endQuote);
    }
}
