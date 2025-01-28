package com.gcfv2pubsub;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

public class YoutubeMailer {

    private static final Logger logger = Logger.getLogger(YoutubeMailer.class.getName());

    public MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);

        return email;
    }

    public Message sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);

        String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);

        return service.users().messages().send(userId, message).execute();
    }

    @SuppressWarnings("deprecation")
    public Gmail getGmailService() throws IOException, GeneralSecurityException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Gmail.Builder(transport, jsonFactory, getCredentials(transport, jsonFactory))
                .setApplicationName("Gmail API For Youtube")
                .build();
    }

    public String getNewToken(String refreshToken, String clientId, String clientSecret) throws IOException {
            ArrayList<String> scopes = new ArrayList<>();
            logger.info(refreshToken);
            logger.info(clientSecret);
            logger.info(clientId);
            scopes.add("https://www.googleapis.com/auth/gmail.compose");
    
            TokenResponse tokenResponse = new GoogleRefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    refreshToken, clientId, clientSecret).setScopes(scopes).setGrantType("refresh_token").execute();
    
            return tokenResponse.getAccessToken();
    }

    @SuppressWarnings("deprecation")
    public Credential getCredentials(HttpTransport httpTransport, JsonFactory jsonFactory) throws GeneralSecurityException, IOException {
        String clientId = System.getenv("CLIENT_ID");
        String clientSecret = System.getenv("CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("CLIENT_ID or CLIENT_SECRET is null. Please set the environment variables.");
        }
        GoogleCredential googleCredential = new GoogleCredential.Builder().setTransport(httpTransport)
                        .setJsonFactory(jsonFactory)
                        .setClientSecrets(clientId, clientSecret)
                        .build();
        String refreshToken = System.getenv("REFRESH_TOKEN");
        googleCredential.setAccessToken(getNewToken(refreshToken, clientId, clientSecret));
        googleCredential.setRefreshToken(refreshToken);
        return googleCredential;
    }
}