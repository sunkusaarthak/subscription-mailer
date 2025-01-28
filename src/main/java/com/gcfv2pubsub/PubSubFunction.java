package com.gcfv2pubsub;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.events.cloud.pubsub.v1.Message;
import com.google.gson.Gson;
import io.cloudevents.CloudEvent;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class PubSubFunction implements CloudEventsFunction {
    private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());
    
    private String[] users = {"sunku.cse@gmail.com", "naraharikrishnadcme@gmail.com",
                            "ganeshjaggu10@gmail.com", "tharunperuru214@gmail.com", 
                            "siri2003.y@gmail.com"};
    
    private String sender = "sunkusaarthak@gmail.com";  

    private static final String YT_SUBJECT = "Youtube Premium Payment Reminder";

    private static final String BUCKET_NAME = "youtube-premium-mailer"; // Replace with your bucket name

    private static final String FILE_NAME = "last_month_payer.txt";

    private final com.google.cloud.storage.Storage storage = StorageOptions.getDefaultInstance().getService();

    private void setCurrentPayerToFile(int content) {
        File tempFile = null;
        try {
            // Create a temporary file
            tempFile = File.createTempFile("last_month_payer", ".txt");
    
            // Write the content to the file
            try (FileWriter fileWriter = new FileWriter(tempFile)) {
                fileWriter.write(Integer.toString(content));
                fileWriter.flush();
            }
    
            // Upload the file to Cloud Storage
            Blob blob = storage.create(
                    Blob.newBuilder(BUCKET_NAME, FILE_NAME).build(),
                    java.nio.file.Files.readAllBytes(tempFile.toPath())
            );
    
            logger.info("File successfully uploaded to Cloud Storage: " + blob.getName());
        } catch (IOException e) {
            logger.severe("Error writing or uploading file to Cloud Storage: " + e.getMessage());
        } finally {
            // Clean up the temporary file
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    logger.info("Temporary file deleted successfully.");
                } else {
                    logger.warning("Failed to delete temporary file.");
                }
            }
        }
    }

    private int getLastMonthPayer() {
        try {
            // Get the file from Cloud Storage
            Blob blob = storage.get(BUCKET_NAME, FILE_NAME);
            if (blob == null || !blob.exists()) {
                logger.info("File not found in Cloud Storage, returning default value 0.");
                return 0;
            }

            // Read file content 
            String content = new String(blob.getContent(), StandardCharsets.UTF_8);
            logger.info("File content: " + content);

            // Parse the content to an integer
            if (!content.isEmpty() && Character.isDigit(content.charAt(0))) {
                return Character.getNumericValue(content.charAt(0));
            } else {
                logger.warning("File content is not a valid digit, returning default value 0.");
            }
        } catch (Exception e) {
            logger.severe("Error reading file from Cloud Storage: " + e.getMessage());
        }
        return 0; // Default value if the file is invalid or missing
    }

    private String getCurrentMonthPayer() {
        int lastMonthIdx = getLastMonthPayer();
        int currentPayer = (lastMonthIdx + 1) % users.length;
        setCurrentPayerToFile(currentPayer);
        return users[currentPayer];
    }

    @Override
    public void accept(CloudEvent event) throws GeneralSecurityException, InterruptedException {
        try {
            // Decode Pub/Sub message
            String cloudEventData = new String(event.getData().toBytes());
            Gson gson = new Gson();
            MessagePublishedData data = gson.fromJson(cloudEventData, MessagePublishedData.class);
            Message message = data.getMessage();
            String encodedData = message.getData();
            String decodedData = new String(Base64.getDecoder().decode(encodedData));

            logger.info("Pub/Sub message: " + decodedData);
            logger.info("Refresh token " + System.getenv("REFRESH_TOKEN"));

            // Prepare and send email
            YoutubeMailer yt = new YoutubeMailer();
            String recipient = getCurrentMonthPayer();
            String YT_BODY = new BuildMail().hitGeminiForBody(recipient);
            MimeMessage email = yt.createEmail(recipient, sender, YT_SUBJECT, YT_BODY);
            
            // Get Gmail Body from Gemini
            com.google.api.services.gmail.model.Message response = yt.sendMessage(yt.getGmailService(), "me", email);
            logger.info("Email sent successfully. Message ID: " + response.getId());
        } catch (MessagingException | IOException e) {
            logger.severe("Error processing the CloudEvent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
