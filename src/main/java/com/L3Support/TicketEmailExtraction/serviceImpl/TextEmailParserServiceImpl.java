package com.L3Support.TicketEmailExtraction.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.L3Support.TicketEmailExtraction.model.BugType;
import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.ContributorRequest;
import com.L3Support.TicketEmailExtraction.model.ContributorResponse;
import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.enums.Project;
import com.L3Support.TicketEmailExtraction.service.ContributorService;
import com.L3Support.TicketEmailExtraction.service.TextEmailParserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TextEmailParserServiceImpl implements TextEmailParserService {

    private final ContributorService contributorService;

    public TextEmailParserServiceImpl(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    // L3 Support team email list from configuration (kept for fallback)
    @Value("${app.l3.allowed.contributors}")
    private String l3SupportTeamConfig;
    
    private List<String> getL3SupportTeam() {
        List<String> team = Arrays.asList(l3SupportTeamConfig.split(","));
        log.info("🔧 L3 Support team configured: {}", team);
        return team;
    }

    // Import Project enum for project extraction
    // Note: Project enum validation will be used instead of hardcoded list

    @Override
    public Ticket parseEmailToTicket(String emailContent) {
        log.info("📧 Starting email parsing for text content...");
        log.info("📧 Raw email content (first 1000 chars): {}", emailContent.length() > 1000 ? emailContent.substring(0, 1000) + "..." : emailContent);
        
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
            
            // Extract the sent date from email headers
            LocalDate sentDate = extractSentDate(emailContent);
            log.info("📅 Sent Date: {}", sentDate);
            
            // Parse ticket information
            String ticketTitle = extractTicketTitle(subject, body);
            log.info("🎫 Ticket Title: {}", ticketTitle);
            
            String ticketDescription = extractTicketDescription(subject, body);
            log.info("📋 Ticket Description length: {} characters", ticketDescription != null ? ticketDescription.length() : 0);
            
            Project project = extractProject(subject, body);
            log.info("🏗️ Project: {}", project.getDisplayName());
            
            Priority priority = extractPriority(subject, body);
            log.info("⚡ Priority: {}", priority);
            
            BugType bugType = extractBugType(subject, body);
            log.info("🐛 Bug Type: {}", bugType);
            
            // Extract impact from body
            String impact = extractImpact(body);
            log.info("💥 Impact: {}", impact);
            
            Status status = Status.OPENED;
            log.info("📊 Status: {}", status);
            
            // Find contributor from L3 support team (but don't auto-assign if multiple found)
            Contributor contributor = findContributor(toEmails);
            log.info("👤 Contributor: {}", contributor != null ? contributor.getName() : "None");
            
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
                .contributorName(contributor != null ? contributor.getName() : null)
                .ticketOwner(ticketOwner)
                .receivedDate(sentDate)
                .impact(impact)
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

    // Extract sent date from email headers
    private LocalDate extractSentDate(String emailContent) {
        log.info("🔍 Extracting sent date from email content...");
        log.info("📧 Email content preview: {}", emailContent.length() > 500 ? emailContent.substring(0, 500) + "..." : emailContent);
        
        // Check if "Sent:" exists in the content at all
        if (emailContent.toLowerCase().contains("sent:")) {
            log.info("✅ 'Sent:' found in email content");
        } else {
            log.warn("❌ 'Sent:' NOT found in email content");
        }
        
        // Try to find Sent header first (most common in your emails)
        Pattern sentPattern = Pattern.compile("(?i)sent:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher sentMatcher = sentPattern.matcher(emailContent);
        
        String dateString = null;
        if (sentMatcher.find()) {
            dateString = sentMatcher.group(1).trim();
            log.info("📅 Found Sent date: '{}'", dateString);
        } else {
            log.warn("⚠️ No 'Sent:' header found, trying 'Date:' header");
            // Try to find Date header as fallback
            Pattern datePattern = Pattern.compile("(?i)date:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
            Matcher dateMatcher = datePattern.matcher(emailContent);
            if (dateMatcher.find()) {
                dateString = dateMatcher.group(1).trim();
                log.info("📅 Found Date header: '{}'", dateString);
            } else {
                log.warn("⚠️ No 'Date:' header found either");
            }
        }
        
        if (dateString != null) {
            LocalDate parsedDate = parseDateString(dateString);
            log.info("📅 Parsed date: {}", parsedDate);
            return parsedDate;
        }
        
        log.warn("⚠️ No date found in email headers, using today's date");
        return LocalDate.now();
    }
    
    // Parse date string to LocalDate
    private LocalDate parseDateString(String dateString) {
        log.info("🔍 Parsing date string: '{}'", dateString);
        
        // Define various date formats that might be encountered in emails
        DateTimeFormatter[] formatters = {
            // "Friday, July 11, 2025 1:14 PM" - Your specific format
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH),
            
            // "Friday, July 11, 2025 13:14" - 24 hour format variation
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm", Locale.ENGLISH),
            
            // "July 11, 2025 1:14 PM" - without day name
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            
            // "15 July 2025 15:21" or "15 July 2025"
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            
            // "15 Jul 2025 15:21" or "15 Jul 2025"
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            
            // "July 15, 2025 15:21" or "July 15, 2025"
            DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm", Locale.ENGLISH),
            
            // "Jul 15, 2025 15:21" or "Jul 15, 2025"
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            
            // "2025-07-15 15:21" or "2025-07-15"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            
            // "15/07/2025 15:21" or "15/07/2025"
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            
            // "07/15/2025 15:21" or "07/15/2025" (US format)
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            
            // RFC 2822 format: "Mon, 15 Jul 2025 15:21:00 +0000"
            new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendOptional(DateTimeFormatter.ofPattern("EEE, ", Locale.ENGLISH))
                .appendPattern("d MMM yyyy HH:mm:ss")
                .appendOptional(DateTimeFormatter.ofPattern(" Z"))
                .toFormatter(Locale.ENGLISH)
        };
        
        // Try each formatter
        for (int i = 0; i < formatters.length; i++) {
            DateTimeFormatter formatter = formatters[i];
            try {
                LocalDate parsedDate = LocalDate.parse(dateString, formatter);
                log.info("✅ Successfully parsed date using formatter {}: {}", i, parsedDate);
                return parsedDate;
            } catch (DateTimeParseException e) {
                log.debug("❌ Formatter {} failed: {}", i, e.getMessage());
                // Continue to next formatter
                continue;
            }
        }
        
        // If no formatter worked, try to extract just the date part using regex
        Pattern dateOnlyPattern = Pattern.compile("(\\d{1,2}\\s+\\w+\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4})");
        Matcher dateOnlyMatcher = dateOnlyPattern.matcher(dateString);
        if (dateOnlyMatcher.find()) {
            String dateOnly = dateOnlyMatcher.group(1);
            log.debug("🔍 Extracted date part: {}", dateOnly);
            return parseDateString(dateOnly); // Recursive call with just the date part
        }
        
        log.warn("⚠️ Failed to parse date string: {}, using today's date", dateString);
        return LocalDate.now();
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

    // Extract project from subject and body using Project enum
    private Project extractProject(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        
        // Check each project's display name and common variations in the content
        for (Project project : Project.values()) {
            if (project == Project.GENERAL) continue; // Skip GENERAL, use as default
            
            String displayName = project.getDisplayName().toLowerCase();
            
            // Check if the project name appears in the content
            if (content.contains(displayName)) {
                return project;
            }
            
            // Check common variations and abbreviations
            switch (project) {
                case MATERIAL_RECEIPT:
                    if (content.contains("material receipt") || content.contains("materialreceipt") ||
                        content.contains("material reciept") || content.contains("materialreciept")) {
                        return project;
                    }
                    break;
                case MY_BUDDY:
                    if (content.contains("my buddy") || content.contains("mybuddy")) {
                        return project;
                    }
                    break;
                case CK_ALUMNI:
                    if (content.contains("ck alumni") || content.contains("ckalumni")) {
                        return project;
                    }
                    break;
                case HEPL_ALUMNI:
                    if (content.contains("hepl alumni") || content.contains("heplalumni")) {
                        return project;
                    }
                    break;
                case HEPL_PORTAL:
                    if (content.contains("hepl portal") || content.contains("heplportal")) {
                        return project;
                    }
                    break;
                case MMW_MODULE_TICKET_TOOL:
                    if (content.contains("mmw module") || content.contains("mmwmodule") ||
                        content.contains("ticket tool") || content.contains("tickettool")) {
                        return project;
                    }
                    break;
                case CK_TRENDS:
                    if (content.contains("ck trends") || content.contains("cktrends")) {
                        return project;
                    }
                    break;
                case LIVEWIRE:
                    if (content.contains("livewire")) {
                        return project;
                    }
                    break;
                case MEETING_AGENDA:
                    if (content.contains("meeting agenda") || content.contains("meetingagenda")) {
                        return project;
                    }
                    break;
                case PRO_HIRE:
                    if (content.contains("pro hire") || content.contains("prohire")) {
                        return project;
                    }
                    break;
                case E_CAPEX:
                    if (content.contains("e-capex") || content.contains("ecapex") || content.contains("e capex")) {
                        return project;
                    }
                    break;
                case SOP:
                    if (content.contains("sop") || content.contains("standard operating procedure")) {
                        return project;
                    }
                    break;
                case ASSET_MANAGEMENT:
                    if (content.contains("asset management") || content.contains("assetmanagement") ||
                        content.contains("assert management") || content.contains("assertmanagement")) {
                        return project;
                    }
                    break;
                case MOULD_MAMP:
                    if (content.contains("mould mamp") || content.contains("mouldmamp")) {
                        return project;
                    }
                    break;
            }
        }
        
        return Project.GENERAL; // Default if no project found
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

    // Extract impact from the middle portion of the email body
    private String extractImpact(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "No impact information available";
        }
        
        // Clean the body content
        String cleanBody = body.trim();
        
        // Split the body into sentences or meaningful chunks
        String[] sentences = cleanBody.split("(?<=[.!?])\\s+");
        
        // If we have multiple sentences, extract the middle portion
        if (sentences.length > 2) {
            int startIndex = sentences.length / 4; // Start from 25% into the content
            int endIndex = (sentences.length * 3) / 4; // End at 75% of the content
            
            // Ensure we have at least one sentence
            if (startIndex >= endIndex) {
                startIndex = sentences.length / 3;
                endIndex = (sentences.length * 2) / 3;
            }
            
            // Make sure indices are valid
            if (startIndex >= sentences.length) startIndex = sentences.length - 1;
            if (endIndex >= sentences.length) endIndex = sentences.length;
            if (startIndex >= endIndex) {
                startIndex = 0;
                endIndex = sentences.length > 1 ? sentences.length / 2 : sentences.length;
            }
            
            StringBuilder impactBuilder = new StringBuilder();
            for (int i = startIndex; i < endIndex; i++) {
                impactBuilder.append(sentences[i]);
                if (i < endIndex - 1) {
                    impactBuilder.append(" ");
                }
            }
            
            String impact = impactBuilder.toString().trim();
            
            // Limit the impact to 500 characters (as per the database column length)
            if (impact.length() > 500) {
                impact = impact.substring(0, 497) + "...";
            }
            
            return impact.isEmpty() ? "No impact information available" : impact;
        } else if (sentences.length == 2) {
            // If only 2 sentences, take the second one as impact
            String impact = sentences[1].trim();
            
            // Limit the impact to 500 characters
            if (impact.length() > 500) {
                impact = impact.substring(0, 497) + "...";
            }
            
            return impact.isEmpty() ? "No impact information available" : impact;
        } else {
            // If no sentence breaks or only one sentence, take the middle portion by character count
            int totalLength = cleanBody.length();
            if (totalLength > 100) {
                int startIndex = totalLength / 4; // Start from 25% of the content
                int endIndex = (totalLength * 3) / 4; // End at 75% of the content
                
                String impact = cleanBody.substring(startIndex, endIndex).trim();
                
                // Limit the impact to 500 characters
                if (impact.length() > 500) {
                    impact = impact.substring(0, 497) + "...";
                }
                
                return impact.isEmpty() ? "No impact information available" : impact;
            } else {
                // If content is too short, return the whole content
                String impact = cleanBody.length() > 500 ? cleanBody.substring(0, 497) + "..." : cleanBody;
                return impact.isEmpty() ? "No impact information available" : impact;
            }
        }
    }

    // Find all contributors from database
    private Contributor findContributor(String toEmails) {
        if (toEmails == null) return null;
        
        log.info("🔍 Searching for contributors in: {}", toEmails);
        
        // Extract all email addresses from the toEmails string using regex
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher emailMatcher = emailPattern.matcher(toEmails.toLowerCase());
        
        List<String> emailsInTo = new ArrayList<>();
        while (emailMatcher.find()) {
            emailsInTo.add(emailMatcher.group(1));
        }
        
        log.info("📧 Extracted emails from TO field: {}", emailsInTo);
        
        // Get all active contributors from database
        List<Contributor> activeContributors = contributorService.getActiveContributors()
                .stream()
                .map(response -> contributorService.getContributorEntityById(response.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        
        log.info("👥 Found {} active contributors in database", activeContributors.size());
        
        List<Contributor> foundContributors = new ArrayList<>();
        
        // Find all matching contributors by email
        for (Contributor contributor : activeContributors) {
            if (contributor.getEmail() != null) {
                String contributorEmail = contributor.getEmail().toLowerCase();
                if (emailsInTo.contains(contributorEmail)) {
                    log.info("✅ Found matching contributor: {} ({})", contributor.getName(), contributor.getEmail());
                    foundContributors.add(contributor);
                }
            }
        }
        
        // If no email matches, try to match by name
        if (foundContributors.isEmpty()) {
            for (Contributor contributor : activeContributors) {
                String contributorName = contributor.getName().toLowerCase();
                for (String email : emailsInTo) {
                    if (email.contains(contributorName.replace(" ", ".")) || 
                        email.contains(contributorName.replace(" ", ""))) {
                        log.info("✅ Found contributor by name pattern: {} ({})", contributor.getName(), email);
          foundContributors.add(contributor);
                        break; // Avoid duplicates for same contributor
                    }
                }
            }
        }
        
        // If multiple contributors found, don't auto-assign - let user choose manually
        if (foundContributors.size() > 1) {
            log.warn("⚠️ Multiple contributors found ({}), not auto-assigning. User should select manually.", 
                foundContributors.stream().map(Contributor::getName).collect(Collectors.joining(", ")));
            return null;
                      }
        
        // If exactly one contributor found, return it
        if (foundContributors.size() == 1) {
            return foundContributors.get(0);
        }
        
        // Final fallback: use configuration-based approach
        log.warn("⚠️ No database contributor found, falling back to configuration-based search");
        return findContributorFromConfig(toEmails);
    }

    // Fallback method using configuration (for backward compatibility)
    private Contributor findContributorFromConfig(String toEmails) {
        List<String> foundContributorNames = new ArrayList<>();
        String toEmailsLower = toEmails.toLowerCase();
        
        // Extract all email addresses from the toEmails string using regex
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher emailMatcher = emailPattern.matcher(toEmailsLower);
        
        List<String> emailsInTo = new ArrayList<>();
        while (emailMatcher.find()) {
            emailsInTo.add(emailMatcher.group(1));
        }
        
        // Check each L3 support team member against extracted emails
        for (String teamMember : getL3SupportTeam()) {
            String trimmedMember = teamMember.trim().toLowerCase();
            if (emailsInTo.contains(trimmedMember)) {
                foundContributorNames.add(teamMember.trim()); // Add original case
                log.info("✅ Found contributor from config: {}", teamMember.trim());
            }
        }
        
        if (!foundContributorNames.isEmpty()) {
            // If multiple contributors found, don't auto-assign
            if (foundContributorNames.size() > 1) {
                log.warn("⚠️ Multiple contributors found in config ({}), not auto-assigning. User should select manually.", 
                    String.join(", ", foundContributorNames));
                return null;
            }
            
            // Try to find or create contributor in database
            String contributorName = foundContributorNames.get(0); // Take first match
            return findOrCreateContributorByName(contributorName);
        }
        
        return null;
    }

    // Helper method to find or create contributor by name
    private Contributor findOrCreateContributorByName(String name) {
        try {
            // Try to find existing contributor by name
            List<Contributor> existingContributors = contributorService.searchContributorsByName(name)
                    .stream()
                    .map(response -> contributorService.getContributorEntityById(response.getId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            
            if (!existingContributors.isEmpty()) {
                return existingContributors.get(0);
            }
            
            // Create new contributor if not found
            log.info("🆕 Creating new contributor: {}", name);
            ContributorRequest newContributorRequest = ContributorRequest.builder()
                    .name(name)
                    .active(true)
                    .department("L3 Support")
                    .notes("Auto-created from email parsing")
                    .build();
            
            ContributorResponse created = contributorService.createContributor(newContributorRequest);
            return contributorService.getContributorEntityById(created.getId()).orElse(null);
            
        } catch (Exception e) {
            log.error("Error finding/creating contributor: {}", name, e);
            return null;
        }
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