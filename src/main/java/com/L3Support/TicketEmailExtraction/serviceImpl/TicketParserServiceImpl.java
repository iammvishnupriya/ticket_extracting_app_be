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

import com.L3Support.TicketEmailExtraction.enums.Project;
import com.L3Support.TicketEmailExtraction.model.BugType;
import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.service.ContributorService;
import com.L3Support.TicketEmailExtraction.service.TicketParserService;
import com.L3Support.TicketEmailExtraction.utils.CommonConstant;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TicketParserServiceImpl implements TicketParserService {

    private final ContributorService contributorService;

    public TicketParserServiceImpl(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    // L3 Support team is now retrieved from database only
    // Configuration-based approach removed - all contributors managed via database

    // Project list now sourced from CommonConstant
    private final List<String> validProjects = CommonConstant.L3_ALLOWED_PROJECTS;

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
            Project project = determineProject(content);
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
            String contributorName = findContributor(toEmails);
            log.info("👤 Contributors: {}", contributorName);
            
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
            
            // Extract the sent date from email headers
            LocalDate sentDate = extractSentDate(content);
            log.info("📅 Sent Date: {}", sentDate);

            // Validate required fields
            if (ticketSummary == null || ticketSummary.trim().isEmpty()) {
                log.warn("⚠️ Missing required field: Ticket Summary");
                return null;
            }

            Ticket ticket = Ticket.builder()
                    .ticketSummary(ticketSummary)
                    .project(project)
                    .issueDescription(issueDescription)
                    .receivedDate(sentDate)
                    .priority(priority)
                    .ticketOwner(ticketOwner)
                    .contributor(null) // Will be set later when we have database integration
                    .contributorName(contributorName)
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
    private Project determineProject(String content) {
        String contentLower = content.toLowerCase();
        
        log.debug("🔍 Analyzing content for project determination: {}", 
                 contentLower.length() > 200 ? contentLower.substring(0, 200) + "..." : contentLower);
        
        // First, try direct matching with project display names
        for (String projectName : validProjects) {
            if (contentLower.contains(projectName.toLowerCase())) {
                Project project = Project.fromString(projectName);
                log.info("🎯 Project found by direct name match '{}': {}", projectName, project);
                return project;
            }
        }
        
        // Enhanced keyword-based matching for better accuracy
        // Material Receipt variations
        if (contentLower.contains("material receipt") || contentLower.contains("materialreceipt") ||
            contentLower.contains("material reciept") || contentLower.contains("materialreciept")) {
            log.info("🎯 Project found by keyword matching: MATERIAL_RECEIPT");
            return Project.MATERIAL_RECEIPT;
        }
        
        // My Buddy variations
        if (contentLower.contains("my buddy") || contentLower.contains("mybuddy")) {
            log.info("🎯 Project found by keyword matching: MY_BUDDY");
            return Project.MY_BUDDY;
        }
        
        // Alumni projects - prioritize HEPL Alumni if both HEPL and Alumni are present
        if (contentLower.contains("alumni")) {
            if (contentLower.contains("hepl")) {
                log.info("🎯 Project found by keyword matching: HEPL_ALUMNI");
                return Project.HEPL_ALUMNI;
            } else if (contentLower.contains("ck")) {
                log.info("🎯 Project found by keyword matching: CK_ALUMNI");
                return Project.CK_ALUMNI;
            } else {
                log.info("🎯 Project found by keyword matching (default alumni): CK_ALUMNI");
                return Project.CK_ALUMNI;
            }
        }
        
        // HEPL Portal (when HEPL is mentioned but not alumni)
        if (contentLower.contains("hepl") && !contentLower.contains("alumni")) {
            log.info("🎯 Project found by keyword matching: HEPL_PORTAL");
            return Project.HEPL_PORTAL;
        }
        
        // MMW Module / Ticket Tool
        if (contentLower.contains("mmw module") || contentLower.contains("mmwmodule") ||
            contentLower.contains("ticket tool") || contentLower.contains("tickettool")) {
            log.info("🎯 Project found by keyword matching: MMW_MODULE_TICKET_TOOL");
            return Project.MMW_MODULE_TICKET_TOOL;
        }
        
        // CK Trends
        if (contentLower.contains("ck trends") || contentLower.contains("cktrends")) {
            log.info("🎯 Project found by keyword matching: CK_TRENDS");
            return Project.CK_TRENDS;
        }
        
        // Livewire
        if (contentLower.contains("livewire")) {
            log.info("🎯 Project found by keyword matching: LIVEWIRE");
            return Project.LIVEWIRE;
        }
        
        // Meeting Agenda
        if (contentLower.contains("meeting agenda") || contentLower.contains("meetingagenda")) {
            log.info("🎯 Project found by keyword matching: MEETING_AGENDA");
            return Project.MEETING_AGENDA;
        }
        
        // Pro Hire
        if (contentLower.contains("pro hire") || contentLower.contains("prohire")) {
            log.info("🎯 Project found by keyword matching: PRO_HIRE");
            return Project.PRO_HIRE;
        }
        
        // E-Capex
        if (contentLower.contains("e-capex") || contentLower.contains("ecapex") || 
            contentLower.contains("e capex") || contentLower.contains("capex")) {
            log.info("🎯 Project found by keyword matching: E_CAPEX");
            return Project.E_CAPEX;
        }
        
        // SOP
        if (contentLower.contains("sop") || contentLower.contains("standard operating procedure")) {
            log.info("🎯 Project found by keyword matching: SOP");
            return Project.SOP;
        }
        
        // Asset Management (including common misspelling)
        if (contentLower.contains("asset management") || contentLower.contains("assetmanagement") ||
            contentLower.contains("assert management") || contentLower.contains("assertmanagement")) {
            log.info("🎯 Project found by keyword matching: ASSET_MANAGEMENT");
            return Project.ASSET_MANAGEMENT;
        }
        
        // Mould Mamp
        if (contentLower.contains("mould mamp") || contentLower.contains("mouldmamp")) {
            log.info("🎯 Project found by keyword matching: MOULD_MAMP");
            return Project.MOULD_MAMP;
        }
        
        // E-Library
        if (contentLower.contains("e-library") || contentLower.contains("elibrary") || 
            contentLower.contains("e library")) {
            log.info("🎯 Project found by keyword matching: E_LIBRARY");
            return Project.E_LIBRARY;
        }
        
        // Outlet Approval
        if (contentLower.contains("outlet_approval") || contentLower.contains("outlet approval") ||
            contentLower.contains("outletapproval")) {
            log.info("🎯 Project found by keyword matching: OUTLET_APPROVAL");
            return Project.OUTLET_APPROVAL;
        }
        
        // RA Tool
        if (contentLower.contains("ra_tool") || contentLower.contains("ra tool") ||
            contentLower.contains("ratool")) {
            log.info("🎯 Project found by keyword matching: RA_TOOL");
            return Project.RA_TOOL;
        }
        
        // CK Bakery
        if (contentLower.contains("ck_bakery") || contentLower.contains("ck bakery") ||
            contentLower.contains("ckbakery")) {
            log.info("🎯 Project found by keyword matching: CK_BAKERY");
            return Project.CK_BAKERY;
        }
        
        // I-View
        if (contentLower.contains("i-view") || contentLower.contains("iview") || 
            contentLower.contains("i view")) {
            log.info("🎯 Project found by keyword matching: I_VIEW");
            return Project.I_VIEW;
        }
        
        // CKPL
        if (contentLower.contains("formbuilder")) {
            log.info("🎯 Project found by keyword matching: CKPL");
            return Project.FORM_BUILDER;
        }

        log.info("🎯 No specific project found, using default: GENERAL");
        return Project.GENERAL; // Default project
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

    // Find all contributors from database
    private String findContributor(String toEmails) {
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
        
        List<String> foundContributorNames = new ArrayList<>();
        
        // Find matching contributors by email
        for (Contributor contributor : activeContributors) {
            if (contributor.getEmail() != null) {
                String contributorEmail = contributor.getEmail().toLowerCase();
                if (emailsInTo.contains(contributorEmail)) {
                    log.info("✅ Found matching contributor: {} ({})", contributor.getName(), contributor.getEmail());
                    foundContributorNames.add(contributor.getName());
                }
            }
        }
        
        // If no email matches, try to match by name pattern
        if (foundContributorNames.isEmpty()) {
            for (Contributor contributor : activeContributors) {
                String contributorName = contributor.getName().toLowerCase();
                for (String email : emailsInTo) {
                    if (email.contains(contributorName.replace(" ", ".")) || 
                        email.contains(contributorName.replace(" ", ""))) {
                        log.info("✅ Found contributor by name pattern: {} ({})", contributor.getName(), email);
                        foundContributorNames.add(contributor.getName());
                        break; // Avoid duplicates for same contributor
                    }
                }
            }
        }
        
        log.info("📋 Total contributors found: {}", foundContributorNames.size());
        
        if (foundContributorNames.isEmpty()) {
            log.warn("⚠️ No contributor found in database for emails: {}", emailsInTo);
            return null;
        }
        
        // Return all contributors as comma-separated string
        return String.join(",", foundContributorNames);
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

    // Extract sent date from email headers
    private LocalDate extractSentDate(String emailContent) {
        log.debug("🔍 Extracting sent date from email content");
        
        // Try to find Sent header first (most common in your emails)
        Pattern sentPattern = Pattern.compile("(?i)sent:\\s*([^\\n\\r]+)", Pattern.MULTILINE);
        Matcher sentMatcher = sentPattern.matcher(emailContent);
        
        String dateString = null;
        if (sentMatcher.find()) {
            dateString = sentMatcher.group(1).trim();
            log.debug("📅 Found Sent date: '{}'", dateString);
        } else {
            // Try to find Date header as fallback
            Pattern datePattern = Pattern.compile("(?i)date:\\s*([^\\n\\r]+)", Pattern.MULTILINE);
            Matcher dateMatcher = datePattern.matcher(emailContent);
            if (dateMatcher.find()) {
                dateString = dateMatcher.group(1).trim();
                log.debug("📅 Found Date header: '{}'", dateString);
            }
        }
        
        if (dateString != null && !dateString.isEmpty()) {
            LocalDate parsedDate = parseDateString(dateString);
            if (parsedDate != null) {
                log.info("✅ Successfully extracted date: {}", parsedDate);
                return parsedDate;
            }
        }
        
        log.warn("⚠️ No date found in email headers, using today's date");
        return LocalDate.now();
    }
    
    // Parse date string to LocalDate
    private LocalDate parseDateString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            log.warn("⚠️ Empty date string provided");
            return null;
        }
        
        dateString = dateString.trim();
        log.debug("🔍 Parsing date string: '{}'", dateString);
        
        // Define various date formats that might be encountered in emails
        DateTimeFormatter[] formatters = {
            // "12 July 2025 10:26" - Your specific format from sample
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            
            // "Friday, July 11, 2025 1:14 PM" - Your other specific format
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH),
            
            // "Friday, July 11, 2025 13:14" - 24 hour format variation
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm", Locale.ENGLISH),
            
            // "July 11, 2025 1:14 PM" - without day name
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            
            // "15 Jul 2025 15:21" or "15 Jul 2025" - abbreviated month
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
                log.debug("✅ Successfully parsed date '{}' using formatter {}: {}", dateString, formatter);
                return parsedDate;
            } catch (DateTimeParseException e) {
                log.debug("❌ Formatter {} failed for '{}': {}", i, dateString, e.getMessage());
                continue;
            }
        }
        
        // If no formatter worked, try to extract just the date part using regex
        Pattern dateOnlyPattern = Pattern.compile("(\\d{1,2}\\s+\\w+\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4})");
        Matcher dateOnlyMatcher = dateOnlyPattern.matcher(dateString);
        if (dateOnlyMatcher.find()) {
            String dateOnly = dateOnlyMatcher.group(1);
            log.debug("🔍 Extracted date part: '{}' from '{}'", dateOnly, dateString);
            // Avoid infinite recursion by checking if we're extracting the same string
            if (!dateOnly.equals(dateString)) {
                return parseDateString(dateOnly); // Recursive call with just the date part
            }
        }
        
        log.warn("⚠️ Failed to parse date string: '{}' with all available formatters", dateString);
        return null;
    }
}
