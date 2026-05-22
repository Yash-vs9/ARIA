package com.yash.nerve.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Service
public class GmailService {

    private static final String APPLICATION_NAME = "ARIA";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR = "tokens";
    private Credential credential;

    private static final List<String> SCOPES = List.of(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            "https://www.googleapis.com/auth/calendar"

    );
    public Credential getCredential() {
        return credential;
    }

    private Gmail gmail;

    @PostConstruct
    public void init() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        gmail = new Gmail.Builder(transport, JSON_FACTORY, getCredentials(transport))
                .setApplicationName(APPLICATION_NAME)
                .build();
        System.out.println(" Gmail service initialized");
    }

    private Credential getCredentials(NetHttpTransport transport) throws Exception {
        // Load credentials.json from resources
        InputStream in = GmailService.class.getResourceAsStream("/credentials.json");
        if (in == null) throw new FileNotFoundException("credentials.json not found in resources");

        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build OAuth flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();

        // First run → opens browser for OAuth consent
        // Subsequent runs → uses saved token
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();
        this.credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        return this.credential;

    }

    // Fetch unread emails
    @Tool(description = "Get unread emails from Gmail. Returns sender, subject and date of unread emails.")

    public String getUnreadEmails(int maxResults) {
        try {
            List<Message> messages = gmail.users().messages()
                    .list("me")
                    .setQ("is:unread")
                    .setMaxResults((long) maxResults)
                    .execute()
                    .getMessages();

            if (messages == null || messages.isEmpty()) {
                return "No unread emails.";
            }

            StringBuilder result = new StringBuilder();
            result.append("Unread emails (").append(messages.size()).append("):\n\n");

            for (Message msg : messages) {
                Message full = gmail.users().messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("From", "Subject", "Date"))
                        .execute();

                String from = getHeader(full, "From");
                String subject = getHeader(full, "Subject");
                String date = getHeader(full, "Date");

                result.append("ID: ").append(full.getId()).append("\n");
                result.append("From: ").append(getHeader(full, "From")).append("\n");
                result.append("Subject: ").append(getHeader(full, "Subject")).append("\n");
                result.append("Date: ").append(getHeader(full, "Date")).append("\n");
                result.append("---\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Error fetching emails: " + e.getMessage();
        }
    }
    @Tool(description = "Read the full body/content of a specific email. Provide the email message ID obtained from getUnreadEmails.")
    public String getEmailBody(String messageId) {
        try {
            Message message = gmail.users().messages()
                    .get("me", messageId)
                    .setFormat("full")
                    .execute();

            // Extract body from payload
            if (message.getPayload() == null) return "Email body not found.";

            String body = extractBody(message.getPayload());
            return body.isEmpty() ? "Email body is empty." : body;

        } catch (Exception e) {
            return "Error reading email body: " + e.getMessage();
        }
    }

    private String extractBody(com.google.api.services.gmail.model.MessagePart payload) {
        // Direct body
        if (payload.getBody() != null && payload.getBody().getData() != null) {
            byte[] decoded = Base64.getUrlDecoder().decode(payload.getBody().getData());
            return new String(decoded);
        }

        // Multipart — search parts
        if (payload.getParts() != null) {
            for (com.google.api.services.gmail.model.MessagePart part : payload.getParts()) {
                if (part.getMimeType().equals("text/plain") && part.getBody().getData() != null) {
                    byte[] decoded = Base64.getUrlDecoder().decode(part.getBody().getData());
                    return new String(decoded);

                }
            }
        }

        return "Could not extract email body.";
    }
    // Search emails
    @Tool(description = "Search emails in Gmail. Provide a search query like 'from:rahul' or 'subject:meeting' or 'invoice'.")

    public String searchEmails(String query, int maxResults) {
        try {
            List<Message> messages = gmail.users().messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults((long) maxResults)
                    .execute()
                    .getMessages();

            if (messages == null || messages.isEmpty()) {
                return "No emails found for: " + query;
            }

            StringBuilder result = new StringBuilder();
            result.append("Found ").append(messages.size()).append(" emails:\n\n");

            for (Message msg : messages) {
                Message full = gmail.users().messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("From", "Subject", "Date"))
                        .execute();

                result.append("From: ").append(getHeader(full, "From")).append("\n");
                result.append("Subject: ").append(getHeader(full, "Subject")).append("\n");
                result.append("Date: ").append(getHeader(full, "Date")).append("\n");
                result.append("---\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Error searching emails: " + e.getMessage();
        }
    }

    // Send email
    @Tool(description = "Send an email via Gmail. Provide recipient email address, subject, and body text.")

    public String sendEmail(String to, String subject, String body) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress("me"));
            email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            email.setSubject(subject);
            email.setText(body);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawBytes = buffer.toByteArray();
            String encodedEmail = Base64.getUrlEncoder().encodeToString(rawBytes);

            Message message = new Message();
            message.setRaw(encodedEmail);

            gmail.users().messages().send("me", message).execute();
            return "Email sent successfully to: " + to;

        } catch (Exception e) {
            return "Error sending email: " + e.getMessage();
        }
    }

    private String getHeader(Message message, String headerName) {
        if (message.getPayload() == null) return "N/A";
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        if (headers == null) return "N/A";
        return headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase(headerName))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("N/A");
    }
}