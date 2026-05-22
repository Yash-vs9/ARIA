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

        NetHttpTransport transport =
                GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = gmailService.getCredential();

        calendar = new Calendar.Builder(
                transport,
                JSON_FACTORY,
                credential
        )
                .setApplicationName(APPLICATION_NAME)
                .build();

        System.out.println("Google Calendar initialized");
    }

    private LocalDateTime parseDateTime(String date, String time) {
        return LocalDateTime.parse(
                date + "T" + time,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        );
    }

    private EventDateTime buildEventDateTime(LocalDateTime dateTime) {

        return new EventDateTime()
                .setDateTime(
                        new DateTime(
                                Date.from(
                                        dateTime.atZone(
                                                ZoneId.systemDefault()
                                        ).toInstant()
                                )
                        )
                )
                .setTimeZone(
                        ZoneId.systemDefault().toString()
                );
    }

    @Tool(description = """
            Create a calendar event.
            Use for reminders, appointments, tasks, birthdays,
            interviews, deadlines, or personal events.
            Does NOT create a Google Meet link.
            """)
    public String createEvent(
            String title,
            String date,
            String time,
            int durationMinutes
    ) {

        try {

            LocalDateTime startTime =
                    parseDateTime(date, time);

            LocalDateTime endTime =
                    startTime.plusMinutes(durationMinutes);

            Event event = new Event()
                    .setSummary(title)
                    .setStart(buildEventDateTime(startTime))
                    .setEnd(buildEventDateTime(endTime));

            Event created =
                    calendar.events()
                            .insert("primary", event)
                            .execute();

            return """
                    Event created successfully

                    Event Id: %s
                    Title: %s
                    Date: %s
                    Time: %s
                    """
                    .formatted(
                            created.getId(),
                            title,
                            date,
                            time
                    );

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = """
            Create a Google Meet meeting and invite attendees.
            Generates a Google Meet link automatically.
            """)
    public String createMeetMeeting(
            String title,
            String date,
            String time,
            int durationMinutes,
            String attendeeEmail
    ) {

        try {

            LocalDateTime startTime =
                    parseDateTime(date, time);

            LocalDateTime endTime =
                    startTime.plusMinutes(durationMinutes);

            Event event = new Event()
                    .setSummary(title)
                    .setStart(buildEventDateTime(startTime))
                    .setEnd(buildEventDateTime(endTime));

            EventAttendee attendee =
                    new EventAttendee()
                            .setEmail(attendeeEmail);

            event.setAttendees(
                    Arrays.asList(attendee)
            );

            ConferenceData conferenceData =
                    new ConferenceData();

            CreateConferenceRequest conferenceRequest =
                    new CreateConferenceRequest();

            conferenceRequest.setRequestId(
                    "meet-" + System.currentTimeMillis()
            );

            conferenceData.setCreateRequest(
                    conferenceRequest
            );

            event.setConferenceData(
                    conferenceData
            );

            Event created =
                    calendar.events()
                            .insert("primary", event)
                            .setConferenceDataVersion(1)
                            .execute();

            return """
                    Google Meet created successfully

                    Event Id: %s
                    Meet Link: %s
                    Attendee: %s
                    """
                    .formatted(
                            created.getId(),
                            created.getHangoutLink(),
                            attendeeEmail
                    );

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get all events scheduled for today.
            """)
    public String getTodayEvents() {

        try {

            DateTime startOfDay = new DateTime(
                    Date.from(
                            LocalDateTime.now()
                                    .toLocalDate()
                                    .atStartOfDay(
                                            ZoneId.systemDefault()
                                    )
                                    .toInstant()
                    )
            );

            DateTime endOfDay = new DateTime(
                    Date.from(
                            LocalDateTime.now()
                                    .toLocalDate()
                                    .plusDays(1)
                                    .atStartOfDay(
                                            ZoneId.systemDefault()
                                    )
                                    .toInstant()
                    )
            );

            Events events =
                    calendar.events()
                            .list("primary")
                            .setTimeMin(startOfDay)
                            .setTimeMax(endOfDay)
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .execute();

            if (events.getItems().isEmpty()) {
                return "No events scheduled today.";
            }

            StringBuilder sb =
                    new StringBuilder("Today's Events\n\n");

            for (Event event : events.getItems()) {

                sb.append("ID: ")
                        .append(event.getId())
                        .append("\n");

                sb.append("Title: ")
                        .append(event.getSummary())
                        .append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = """
            Get upcoming events for the next specified number of days.
            """)
    public String getUpcomingEvents(int days) {

        try {

            DateTime now =
                    new DateTime(new Date());

            DateTime future =
                    new DateTime(
                            Date.from(
                                    LocalDateTime.now()
                                            .plusDays(days)
                                            .atZone(
                                                    ZoneId.systemDefault()
                                            )
                                            .toInstant()
                            )
                    );

            Events events =
                    calendar.events()
                            .list("primary")
                            .setTimeMin(now)
                            .setTimeMax(future)
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .execute();

            if (events.getItems().isEmpty()) {
                return "No upcoming events found.";
            }

            StringBuilder sb =
                    new StringBuilder();

            for (Event event : events.getItems()) {

                sb.append("ID: ")
                        .append(event.getId())
                        .append("\n");

                sb.append("Title: ")
                        .append(event.getSummary())
                        .append("\n");

                sb.append("-----------------\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = """
            Search events by title keyword.
            Returns matching event IDs.
            """)
    public String findEvents(String keyword) {

        try {

            Events events =
                    calendar.events()
                            .list("primary")
                            .setQ(keyword)
                            .execute();

            if (events.getItems().isEmpty()) {
                return "No matching events found.";
            }

            StringBuilder sb =
                    new StringBuilder();

            for (Event event : events.getItems()) {

                sb.append("ID: ")
                        .append(event.getId())
                        .append("\n");

                sb.append("Title: ")
                        .append(event.getSummary())
                        .append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool(description = """
            Delete a calendar event using its event ID.
            """)
    public String deleteEvent(String eventId) {

        try {

            calendar.events()
                    .delete("primary", eventId)
                    .execute();

            return "Event deleted successfully.";

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}