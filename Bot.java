import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class Bot {
    public static void main(String[] args) {
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");
        
        String modelName = "mixtral-8x7b-32768";
        String llmRequestBody = """
            {"messages":[{"role":"user","content":"%s"}],"model":"%s"}
            """.formatted(message.replace("\"", "\\\""), modelName);
            
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + llmKey)
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody))
            .build();
            
        try {
            HttpResponse<String> llmResponse = llmClient.send(llmRequest, HttpResponse.BodyHandlers.ofString());
            String llmBody = llmResponse.body();
            
            int messageStart = llmBody.indexOf("\"message\":{");
            int contentStart = llmBody.indexOf("\"content\":\"", messageStart) + 10;
            int contentEnd = llmBody.indexOf("\"},\"logprobs\"");
            String content = llmBody.substring(contentStart, contentEnd).replace("\\\"", "\"");
            
            // 앞뒤에 있는 불필요한 큰 따옴표만 제거 (문자열의 시작(^)과 끝($)에 있는 " 문자 제거)
            content = content.replaceAll("^\"+|\"+$", "");
            
            String slackJson = """
                {"text":"%s"}
                """.formatted(content.replace("\"", "\\\""));
                
            HttpClient slackClient = HttpClient.newHttpClient();
            HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(slackJson))
                .build();
                
            HttpResponse<String> slackResponse = slackClient.send(slackRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
