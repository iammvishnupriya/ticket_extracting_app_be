package com.L3Support.TicketEmailExtraction.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.L3Support.TicketEmailExtraction.model.BugType;
import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.service.TextEmailParserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TextEmailParserServiceImpl implements TextEmailParserService {

    // L3 Support team email list from configuration
    @Value("${app.l3.allowed.contributors}")
    private String l3SupportTeamConfig;
    
    private List<String> getL3SupportTeam() {
        List<String> team = Arrays.asList(l3SupportTeamConfig.split(","));
        log.info("🔧 L3 Support team configured: {}", team);
        return team;
    }

    // Project list - you can provide this list
    private final List<String> validProjects = Arrays.asList(
        "HEPL Portal",
        "HEPL Mobile App",
        "HEPL Backend",
        "HEPL API",
        "HEPL Database",
        "HEPL Infrastructure",
        "HEPL Integration",
        "HEPL Security",
        "HEPL Analytics",
        "HEPL Reporting"
        // Add more valid projects here
    );

    @Override
    public Ticket parseEmailToTicket(String emailContent) {
        log.info("📧 Starting email parsing for text content...");
        
        try {
            // Extract basic email information
            String subject = extractSubject(emailContent);
            log.info("📝 Subject: {}", subject);
            
            String fromEmail = extractFromEmail(emailContent);
            log.info("📤 From: {}", fromEmail);
            
            String toEmails = extractToEmails(emailContent);
            log.info("📥 To: {}", toEmails);
            
            String body = extractBody(emailContent);
            log.info("📄 Body length: {} characters", body != null ? body.length() : 0);
            
            // Parse ticket information
            String ticketTitle = extractTicketTitle(subject, body);
            log.info("🎫 Ticket Title: {}", ticketTitle);
            
            String ticketDescription = extractTicketDescription(subject, body);
            log.info("📋 Ticket Description length: {} characters", ticketDescription != null ? ticketDescription.length() : 0);
            
            String project = extractProject(subject, body);
            log.info("🏗️ Project: {}", project);
            
            Priority priority = extractPriority(subject, body);
            log.info("⚡ Priority: {}", priority);
            
            BugType bugType = extractBugType(subject, body);
            log.info("🐛 Bug Type: {}", bugType);
            
            Status status = Status.OPENED;
            log.info("📊 Status: {}", status);
            
            // Find all contributors from L3 support team
            String contributor = findContributor(toEmails);
            log.info("👤 Contributors: {}", contributor);
            
            // Extract ticket owner (usually the sender)
            String ticketOwner = extractTicketOwner(fromEmail);
            log.info("👤 Ticket Owner: {}", ticketOwner);
            
            // Create and return ticket
            Ticket ticket = Ticket.builder()
                .ticketSummary(ticketTitle)
                .issueDescription(ticketDescription)
                .project(project)
                .priority(priority)
                .bugType(bugType)
                .status(status)
                .contributor(contributor)
                .ticketOwner(ticketOwner)
                .receivedDate(LocalDate.now())
                .build();
                
            log.info("✅ Ticket created successfully: {}", ticket);
            return ticket;
            
        } catch (Exception e) {
            log.error("❌ Error parsing email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse email", e);
        }
    }

    // Extract subject from email content
    private String extractSubject(String emailContent) {
        Pattern pattern = Pattern.compile("(?i)subject:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "No Subject";
    }

    // Extract from email address
    private String extractFromEmail(String emailContent) {
        Pattern pattern = Pattern.compile("(?i)from:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            String fromLine = matcher.group(1).trim();
            // Extract email from format like "Name <email@domain.com>" or just "email@domain.com"
            Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
            Matcher emailMatcher = emailPattern.matcher(fromLine);
            if (emailMatcher.find()) {
                return emailMatcher.group(1);
            }
            return fromLine;
        }
        return null;
    }

    // Extract to email addresses
    private String extractToEmails(String emailContent) {
        Pattern pattern = Pattern.compile("(?i)to:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(emailContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    // Extract email body
    private String extractBody(String emailContent) {
        // Look for common patterns that indicate the start of the body
        String[] bodyStartPatterns = {
            "(?i)\\n\\n", // Double newline after headers
            "(?i)\\nDate:.*?\\n\\n", // After date header
            "(?i)\\nContent-Type:.*?\\n\\n", // After content type
            "(?i)\\nMIME-Version:.*?\\n\\n" // After MIME version
        };
        
        for (String pattern : bodyStartPatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher matcher = p.matcher(emailContent);
            if (matcher.find()) {
                return emailContent.substring(matcher.end()).trim();
            }
        }
        
        // If no pattern found, try to find body after the last header line
        String[] lines = emailContent.split("\\n");
        boolean foundEmptyLine = false;
        StringBuilder bodyBuilder = new StringBuilder();
        
        for (String line : lines) {
            if (foundEmptyLine || line.trim().isEmpty()) {
                if (line.trim().isEmpty()) {
                    foundEmptyLine = true;
                } else {
                    bodyBuilder.append(line).append("\n");
                }
            } else if (!line.contains(":") || (!line.toLowerCase().startsWith("from:") && 
                      !line.toLowerCase().startsWith("to:") && 
                      !line.toLowerCase().startsWith("subject:") && 
                      !line.toLowerCase().startsWith("date:"))) {
                bodyBuilder.append(line).append("\n");
            }
        }
        
        return bodyBuilder.toString().trim();
    }

    // Extract ticket title from subject and body
    private String extractTicketTitle(String subject, String body) {
        if (subject != null && !subject.equals("No Subject")) {
            return subject.length() > 100 ? subject.substring(0, 100) + "..." : subject;
        }
        
        // If no subject, try to extract from first line of body
        if (body != null && !body.trim().isEmpty()) {
            String firstLine = body.split("\\n")[0].trim();
            return firstLine.length() > 100 ? firstLine.substring(0, 100) + "..." : firstLine;
        }
        
        return "Ticket from Email";
    }

    // Extract ticket description from body
    private String extractTicketDescription(String subject, String body) {
        StringBuilder description = new StringBuilder();
        
        if (subject != null && !subject.equals("No Subject")) {
            description.append("Subject: ").append(subject).append("\n\n");
        }
        
        if (body != null && !body.trim().isEmpty()) {
            description.append("Description:\n").append(body);
        }
        
        return description.length() > 0 ? description.toString() : "No description available";
    }

    // Extract project from subject and body
    private String extractProject(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        
        for (String project : validProjects) {
            if (content.contains(project.toLowerCase())) {
                return project;
            }
        }
        
        return "General"; // Default project
    }

    // Extract priority from subject and body
    private Priority extractPriority(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        
        if (content.contains("critical") || content.contains("urgent") || 
            content.contains("high priority") || content.contains("asap")) {
            return Priority.HIGH;
        } else if (content.contains("high") || content.contains("important")) {
            return Priority.HIGH;
        } else if (content.contains("low") || content.contains("minor")) {
            return Priority.LOW;
        } else {
            return Priority.MODERATE; // Default priority
        }
    }

    // Extract bug type from subject and body
    private BugType extractBugType(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        
        if (content.contains("enhancement") || content.contains("feature") || 
            content.contains("improvement") || content.contains("new feature")) {
            return BugType.ENHANCEMENT;
        } else if (content.contains("task") || content.contains("todo") || 
                   content.contains("action item")) {
            return BugType.TASK;
        } else {
            return BugType.BUG; // Default to bug
        }
    }

    // Find all contributors from L3 support team
    private String findContributor(String toEmails) {
        if (toEmails == null) return null;
        
        List<String> foundContributors = new ArrayList<>();
        String toEmailsLower = toEmails.toLowerCase();
        
        log.info("🔍 Searching for contributors in: {}", toEmails);
        
        // Extract all email addresses from the toEmails string using regex
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher emailMatcher = emailPattern.matcher(toEmailsLower);
        
        List<String> emailsInTo = new ArrayList<>();
        while (emailMatcher.find()) {
            emailsInTo.add(emailMatcher.group(1));
        }
        
        log.info("📧 Extracted emails from TO field: {}", emailsInTo);
        
        // Check each L3 support team member against extracted emails
        for (String teamMember : getL3SupportTeam()) {
            String trimmedMember = teamMember.trim().toLowerCase();
            if (emailsInTo.contains(trimmedMember)) {
                foundContributors.add(teamMember.trim()); // Add original case
                log.info("✅ Found contributor: {}", teamMember.trim());
            }
        }
        
        log.info("📋 Total contributors found: {}", foundContributors.size());
        
        // Return all contributors as comma-separated string
        return foundContributors.isEmpty() ? null : String.join(",", foundContributors);
    }

    // Extract ticket owner from sender email
    private String extractTicketOwner(String fromEmail) {
        if (fromEmail != null) {
            // Extract name part from email (everything before @)
            String[] parts = fromEmail.split("@");
            if (parts.length > 0) {
                return parts[0].replace(".", " ").trim();
            }
        }
        return "Unknown";
    }
}