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
import com.L3Support.TicketEmailExtraction.service.TicketParserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TicketParserServiceImpl implements TicketParserService {

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
        "CK_Alumni",
        "I-View",
        "HEPL",
        "CKPL",
        "CavinKare"
        // Add more projects here
    );

    @Override
    public Ticket parseEmailToTicket(String content) {
        try {
            log.debug("🔍 Starting to parse email content");
            
            // Extract email components
            String subject = extractSubject(content);
            String fromEmail = extractFromEmail(content);
            String toEmails = extractToEmails(content);
            String emailBody = extractEmailBody(content);
            
            log.info("📧 Email parsed - Subject: {}, From: {}", subject, fromEmail);
            log.info("📧 To Emails: {}", toEmails);
            log.info("📧 Email Body Preview: {}", emailBody != null ? emailBody.substring(0, Math.min(200, emailBody.length())) : "null");
            
            // Use subject as ticket summary
            String ticketSummary = cleanSubject(subject);
            log.info("🎫 Ticket Summary: {}", ticketSummary);
            
            // Extract issue description from email body
            String issueDescription = extractIssueDescription(emailBody);
            log.info("📝 Issue Description: {}", issueDescription);
            
            // Determine project from email content
            String project = determineProject(content);
            log.info("🏗️ Project: {}", project);
            
            // Determine priority from content
            Priority priority = determinePriority(content);
            log.info("⚡ Priority: {}", priority);
            
            // Determine if it's bug or enhancement
            BugType bugType = determineBugType(content);
            log.info("🐛 Bug Type: {}", bugType);
            
            // Set default status as OPENED for new tickets
            Status status = Status.OPENED;
            log.info("📊 Status: {}", status);
            
            // Find all contributors from L3 support team
            String contributor = findContributor(toEmails);
            log.info("👤 Contributors: {}", contributor);
            
            // Extract ticket owner (usually the sender)
            String ticketOwner = extractTicketOwner(fromEmail);
            log.info("👨‍💼 Ticket Owner: {}", ticketOwner);
            
            // Extract employee details if available
            String employeeId = extractEmployeeId(content);
            String employeeName = extractEmployeeName(content);
            log.info("🆔 Employee ID: {}, Name: {}", employeeId, employeeName);
            
            // Extract contact info
            String contact = extractContact(content);
            log.info("📞 Contact: {}", contact);
            
            // Extract impact/roles
            String impact = extractImpact(content);
            log.info("💥 Impact: {}", impact);

            // Validate required fields
            if (ticketSummary == null || ticketSummary.trim().isEmpty()) {
                log.warn("⚠️ Missing required field: Ticket Summary");
                return null;
            }

            Ticket ticket = Ticket.builder()
                    .ticketSummary(ticketSummary)
                    .project(project)
                    .issueDescription(issueDescription)
                    .receivedDate(LocalDate.now())
                    .priority(priority)
                    .ticketOwner(ticketOwner)
                    .contributor(contributor)
                    .bugType(bugType)
                    .status(status)
                    .contact(contact)
                    .impact(impact)
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .build();

            log.debug("✅ Successfully parsed ticket with summary: {}", ticketSummary);
            return ticket;

        } catch (Exception e) {
            log.error("❌ Error parsing email to ticket: {}", e.getMessage(), e);
            return null;
        }
    }

    // Extract email subject
    private String extractSubject(String content) {
        // Try multiple patterns to extract subject
        Pattern[] patterns = {
            Pattern.compile("Subject:\\s*(.*?)(?=\\n|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Subject:\\s*(.*?)(?=\\s{2,}|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Subject:(.*)(?=\\n|$)", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String subject = matcher.group(1).trim();
                if (!subject.isEmpty()) {
                    log.debug("📧 Raw subject extracted: '{}'", subject);
                    return subject;
                }
            }
        }
        
        log.warn("⚠️ No subject found in email content");
        return "No Subject";
    }

    // Clean subject by removing Fw:, Re:, etc.
    private String cleanSubject(String subject) {
        if (subject == null) return "No Subject";
        String cleaned = subject.replaceAll("^(Re:|Fw:|Fwd:)\\s*", "").trim();
        
        // Limit length to fit database column
        if (cleaned.length() > 490) {
            cleaned = cleaned.substring(0, 490) + "...";
        }
        
        return cleaned;
    }

    // Extract From email
    private String extractFromEmail(String content) {
        Pattern pattern = Pattern.compile("From:\\s*.*?<([^>]+)>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Fallback pattern for simple email format
        pattern = Pattern.compile("From:\\s*([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    // Extract To emails
    private String extractToEmails(String content) {
        Pattern pattern = Pattern.compile("To:\\s*(.*?)(?=\\n|Subject:|Cc:|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    // Extract email body (main content after headers)
    private String extractEmailBody(String content) {
        log.debug("📧 Extracting email body from content length: {}", content.length());
        
        // Find the main body after email headers
        String[] lines = content.split("\\n");
        StringBuilder body = new StringBuilder();
        boolean inBody = false;
        boolean foundFirstContent = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip email headers
            if (trimmedLine.matches("^(From:|To:|Cc:|Sent on:|Subject:|Date:).*")) {
                continue;
            }
            
            // Skip empty lines at the beginning
            if (!foundFirstContent && trimmedLine.isEmpty()) {
                continue;
            }
            
            // Start collecting body after headers
            if (!inBody && !trimmedLine.isEmpty() && !trimmedLine.matches("^(From:|To:|Cc:|Sent on:|Subject:|Date:).*")) {
                inBody = true;
                foundFirstContent = true;
            }
            
            if (inBody) {
                // Stop at confidentiality notice or final signature
                if (trimmedLine.contains("Confidentiality Notice:") || 
                    (trimmedLine.contains("Thanks & Regards") && !trimmedLine.contains("As per")) ||
                    (trimmedLine.contains("Regards,") && !trimmedLine.contains("Thanks"))) {
                    break;
                }
                body.append(line).append("\n");
            }
        }
        
        String result = body.toString().trim();
        log.debug("📧 Extracted email body: {}", result.length() > 200 ? result.substring(0, 200) + "..." : result);
        return result;
    }

    // Extract issue description from email body
    private String extractIssueDescription(String emailBody) {
        if (emailBody == null || emailBody.trim().isEmpty()) {
            return "No description provided";
        }
        
        log.debug("📝 Raw email body: {}", emailBody);
        
        // Split into lines for better processing
        String[] lines = emailBody.split("\\n");
        StringBuilder meaningfulContent = new StringBuilder();
        boolean foundMeaningfulContent = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip empty lines
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // Skip common email patterns and greetings
            if (trimmedLine.matches("^\\+{5,}.*") || // Skip +++++ lines
                trimmedLine.matches("^Dear\\s+\\w+,?.*") || // Skip Dear Name,
                trimmedLine.matches("^Greetings of the day!.*") || // Skip Greetings
                trimmedLine.matches("^Thanks and Regards,.*") || // Skip Thanks and Regards
                trimmedLine.matches("^Thanks & Regards,.*") || // Skip Thanks & Regards
                trimmedLine.matches("^Regards,.*") || // Skip Regards
                trimmedLine.matches("^From:.*") || // Skip From lines
                trimmedLine.matches("^To:.*") || // Skip To lines
                trimmedLine.matches("^Sent:.*") || // Skip Sent lines
                trimmedLine.matches("^Subject:.*") || // Skip Subject lines
                trimmedLine.matches("^Cc:.*")) { // Skip Cc lines
                continue;
            }
            
            // Start collecting meaningful content
            if (!foundMeaningfulContent && !trimmedLine.isEmpty()) {
                foundMeaningfulContent = true;
            }
            
            if (foundMeaningfulContent) {
                meaningfulContent.append(trimmedLine).append(" ");
            }
        }
        
        String cleaned = meaningfulContent.toString().trim();
        
        // Additional cleanup
        cleaned = cleaned
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
        
        // Take first 1900 characters as description (leaving room for database limit)
        if (cleaned.length() > 1900) {
            cleaned = cleaned.substring(0, 1900) + "...";
        }
        
        String result = cleaned.isEmpty() ? "No description provided" : cleaned;
        log.debug("📝 Processed issue description: {}", result);
        return result;
    }

    // Determine project from email content
    private String determineProject(String content) {
        String contentLower = content.toLowerCase();
        
        for (String project : validProjects) {
            if (contentLower.contains(project.toLowerCase())) {
                return project;
            }
        }
        
        // Check for specific keywords
        if (contentLower.contains("i-view") || contentLower.contains("iview")) {
            return "I-View";
        }
        if (contentLower.contains("hrss") || contentLower.contains("hr")) {
            return "HRSS";
        }
        if (contentLower.contains("alumni")) {
            return "CK_Alumni";
        }
        if (contentLower.contains("cavinkare") || contentLower.contains("ckpl")) {
            return "CKPL";
        }
        
        return "General"; // Default project
    }

    // Determine priority from email content
    private Priority determinePriority(String content) {
        String contentLower = content.toLowerCase();
        
        // Check for priority keywords
        if (contentLower.contains("urgent") || contentLower.contains("critical") || 
            contentLower.contains("high priority") || contentLower.contains("asap")) {
            return Priority.HIGH;
        }
        if (contentLower.contains("moderate") || contentLower.contains("medium")) {
            return Priority.MODERATE;
        }
        if (contentLower.contains("low priority") || contentLower.contains("low")) {
            return Priority.LOW;
        }
        if (contentLower.contains("priority")) {
            return Priority.PRIORITY;
        }
        
        return Priority.MODERATE; // Default priority
    }

    // Determine if it's bug or enhancement
    private BugType determineBugType(String content) {
        String contentLower = content.toLowerCase();
        
        // Keywords that suggest enhancement
        if (contentLower.contains("enhancement") || contentLower.contains("feature") ||
            contentLower.contains("improvement") || contentLower.contains("add") ||
            contentLower.contains("new") || contentLower.contains("change")) {
            return BugType.ENHANCEMENT;
        }
        
        // Keywords that suggest bug
        if (contentLower.contains("bug") || contentLower.contains("error") ||
            contentLower.contains("issue") || contentLower.contains("problem") ||
            contentLower.contains("not working") || contentLower.contains("failed")) {
            return BugType.BUG;
        }
        
        return BugType.BUG; // Default to bug
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
        if (fromEmail == null) return null;
        
        // Extract name part before @ symbol
        String[] parts = fromEmail.split("@");
        if (parts.length > 0) {
            return parts[0].replace(".", " ").trim();
        }
        return fromEmail;
    }

    // Extract employee ID from content
    private String extractEmployeeId(String content) {
        // Look for patterns like "Employee ID: EMP001" or "EMP001" or "1015796"
        Pattern pattern = Pattern.compile("(?:Employee ID|EMP|ID)\\s*:?\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Look for numeric employee IDs
        pattern = Pattern.compile("\\b(\\d{6,8})\\b");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    // Extract employee name from content
    private String extractEmployeeName(String content) {
        // Look for patterns like "Employee Name: John Doe"
        Pattern pattern = Pattern.compile("Employee Name\\s*:?\\s*([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Try to extract from signature
        String[] lines = content.split("\\n");
        for (String line : lines) {
            if (line.contains("Thanks & Regards") || line.contains("Regards,")) {
                // Next line might contain name
                int index = Arrays.asList(lines).indexOf(line);
                if (index + 1 < lines.length) {
                    String nextLine = lines[index + 1].trim();
                    if (nextLine.matches("^[A-Za-z\\s]+$") && nextLine.length() > 2 && nextLine.length() < 50) {
                        return nextLine;
                    }
                }
            }
        }
        
        return null;
    }

    // Extract contact information
    private String extractContact(String content) {
        // Look for phone numbers
        Pattern phonePattern = Pattern.compile("(\\d{5}\\s+\\d{6}|\\d{10}|\\+\\d{1,3}\\s?\\d{10})");
        Matcher phoneMatcher = phonePattern.matcher(content);
        if (phoneMatcher.find()) {
            return phoneMatcher.group(1);
        }
        
        // Look for email in signature
        Pattern emailPattern = Pattern.compile("([\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,})");
        Matcher emailMatcher = emailPattern.matcher(content);
        if (emailMatcher.find()) {
            return emailMatcher.group(1);
        }
        
        return null;
    }

    // Extract impact/roles information
    private String extractImpact(String content) {
        // Look for role-related keywords
        if (content.toLowerCase().contains("all users")) {
            return "All users affected";
        }
        if (content.toLowerCase().contains("admin")) {
            return "Admin users affected";
        }
        if (content.toLowerCase().contains("employee")) {
            return "Employee users affected";
        }
        
        return "Impact not specified";
    }

    private Priority parsePriority(String input) {
        if (input == null) return Priority.MODERATE;
        String cleaned = input.trim().toUpperCase();
        
        try {
            return Priority.valueOf(cleaned);
        } catch (Exception e) {
            // Handle variations
            if (cleaned.contains("HIGH") || cleaned.contains("URGENT")) {
                return Priority.HIGH;
            } else if (cleaned.contains("LOW")) {
                return Priority.LOW;
            } else if (cleaned.contains("MODERATE") || cleaned.contains("MEDIUM")) {
                return Priority.MODERATE;
            } else if (cleaned.contains("PRIORITY")) {
                return Priority.PRIORITY;
            }
            return Priority.MODERATE; // Default
        }
    }

    private BugType parseBugType(String input) {
        if (input == null) return BugType.BUG;
        String cleaned = input.trim().toUpperCase();
        
        try {
            return BugType.valueOf(cleaned);
        } catch (Exception e) {
            // Handle variations
            if (cleaned.contains("ENHANCEMENT") || cleaned.contains("FEATURE")) {
                return BugType.ENHANCEMENT;
            } else {
                return BugType.BUG;
            }
        }
    }

    private Status parseStatus(String input) {
        if (input == null) return Status.OPENED;
        String cleaned = input.trim().toUpperCase();
        
        try {
            return Status.valueOf(cleaned);
        } catch (Exception e) {
            // Handle variations
            if (cleaned.contains("ASSIGNED")) {
                return Status.ASSIGNED;
            } else if (cleaned.contains("CLOSED") || cleaned.contains("FIXED")) {
                return Status.CLOSED;
            } else {
                return Status.OPENED; // Default
            }
        }
    }
}
