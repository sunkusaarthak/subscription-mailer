package com.gcfv2pubsub;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BuildMail {
    private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());
    public String hitGeminiForBody(String email) throws IOException, InterruptedException {
        String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
        String PROMPT = "Please give me email body, here are some instructions to generate email body - don't give me anything just email body and use the email to predict the name. my friend with email " + email + ", asking 299 rupees for the Youtube Premium Subscription for this month notifiying that the turn for payment this month is them. Sender is Youtube Premium Mailer";
        PROMPT = PROMPT.replace("\"", "\\\"").replace("\n", "\\n");
        String jsonRequest = """
                {
                    "contents": [
                        {
                            "parts":[
                                {
                                    "text": "%s"
                                }
                            ]
                        }
                    ]
                }
                """.formatted(PROMPT);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info(response.body());
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
        JsonArray parts = content.getAsJsonArray("parts");
        String emailSubjectString = parts.get(0).getAsJsonObject().get("text").getAsString();
        logger.info(emailSubjectString);
        return emailSubjectString;
    }
}
