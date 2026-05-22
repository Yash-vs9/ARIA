package com.yash.nerve.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "ARIA";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private Calendar calendar;
    private final GmailService gmailService;

    public GoogleCalendarService(GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @PostConstruct
    public void init() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        // Reuse same credential from GmailService — no separate OAuth needed
        Credential credential = gmailService.getCredential();
        calendar = new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        System.out.println("✅ Google Calendar service initialized");
    }

    @Tool(description = "Get today's calendar events. Returns list of meetings and events scheduled for today.")
    public String getTodayEvents() {
        try {
            DateTime startOfDay = new DateTime(
                    Date.from(LocalDateTime.now().toLocalDate()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant())
            );
            DateTime endOfDay = new DateTime(
                    Date.from(LocalDateTime.now().toLocalDate()
                            .plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
            );

            Events events = calendar.events().list("primary")
                    .setTimeMin(startOfDay)
                    .setTimeMax(endOfDay)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            if (items.isEmpty()) return "No events scheduled for today.";

            StringBuilder result = new StringBuilder("Today's events:\n\n");
            for (Event event : items) {
                String start = event.getStart().getDateTime() != null
                        ? event.getStart().getDateTime().toString()
                        : event.getStart().getDate().toString();
                result.append("📅 ").append(event.getSummary()).append("\n");
                result.append("   Time: ").append(start).append("\n");
                if (event.getHangoutLink() != null) {
                    result.append("   Meet: ").append(event.getHangoutLink()).append("\n");
                }
                result.append("\n");
            }
            return result.toString();

        } catch (Exception e) {
            return "Error fetching events: " + e.getMessage();
        }
    }

    @Tool(description = "Create a Google Meet meeting. Provide title, date (YYYY-MM-DD), time (HH:MM), duration in minutes, and attendee email.")
    public String createMeeting(String title, String date, String time,
                                int durationMinutes, String attendeeEmail) {
        try {
            // Parse date and time
            LocalDateTime startDateTime = LocalDateTime.parse(
                    date + "T" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            );
            LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

            // Build event
            Event event = new Event().setSummary(title);

            // Set start time
            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(
                            Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant())
                    ))
                    .setTimeZone(ZoneId.systemDefault().toString());
            event.setStart(start);

            // Set end time
            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(
                            Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant())
                    ))
                    .setTimeZone(ZoneId.systemDefault().toString());
            event.setEnd(end);

            // Add attendee
            EventAttendee attendee = new EventAttendee().setEmail(attendeeEmail);
            event.setAttendees(Arrays.asList(attendee));

            // Add Google Meet conference
            ConferenceData conferenceData = new ConferenceData();
            CreateConferenceRequest conferenceRequest = new CreateConferenceRequest();
            conferenceRequest.setRequestId("meet-" + System.currentTimeMillis());
            conferenceData.setCreateRequest(conferenceRequest);
            event.setConferenceData(conferenceData);

            // Insert event
            Event createdEvent = calendar.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute();

            String meetLink = createdEvent.getHangoutLink();
            return "✅ Meeting created successfully!\n" +
                    "Title: " + title + "\n" +
                    "Date: " + date + " at " + time + "\n" +
                    "Duration: " + durationMinutes + " minutes\n" +
                    "Attendee: " + attendeeEmail + "\n" +
                    "Meet Link: " + (meetLink != null ? meetLink : "No Meet link generated");

        } catch (Exception e) {
            return "Error creating meeting: " + e.getMessage();
        }
    }

    @Tool(description = "Get upcoming calendar events for the next N days.")
    public String getUpcomingEvents(int days) {
        try {
            DateTime now = new DateTime(new Date());
            DateTime future = new DateTime(
                    Date.from(LocalDateTime.now().plusDays(days)
                            .atZone(ZoneId.systemDefault()).toInstant())
            );

            Events events = calendar.events().list("primary")
                    .setTimeMin(now)
                    .setTimeMax(future)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();
            if (items.isEmpty()) return "No upcoming events in the next " + days + " days.";

            StringBuilder result = new StringBuilder();
            result.append("Upcoming events (next ").append(days).append(" days):\n\n");

            for (Event event : items) {
                String start = event.getStart().getDateTime() != null
                        ? event.getStart().getDateTime().toString()
                        : event.getStart().getDate().toString();
                result.append("📅 ").append(event.getSummary()).append("\n");
                result.append("   ").append(start).append("\n");
                if (event.getHangoutLink() != null) {
                    result.append("   Meet: ").append(event.getHangoutLink()).append("\n");
                }
                result.append("\n");
            }
            return result.toString();

        } catch (Exception e) {
            return "Error fetching upcoming events: " + e.getMessage();
        }
    }
}