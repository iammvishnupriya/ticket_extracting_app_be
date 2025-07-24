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

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

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
    
    // Fuzzy matching utilities
    private final JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    
    // Configurable fuzzy matching thresholds
    @Value("${app.fuzzy.project.similarity.threshold:0.75}")
    private double projectSimilarityThreshold;
    
    @Value("${app.fuzzy.priority.similarity.threshold:0.80}")
    private double prioritySimilarityThreshold;
    
    @Value("${app.fuzzy.enable.logging:true}")
    private boolean enableFuzzyLogging;

    public TextEmailParserServiceImpl(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    // L3 Support team is now retrieved from database only
    // Configuration-based approach removed - all contributors managed via database

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

    // Extract sent date from email headers - Enhanced for multiple formats
    private LocalDate extractSentDate(String emailContent) {
        log.info("🔍 Extracting sent date from email content...");
        log.info("📧 Email content preview: {}", emailContent.length() > 500 ? emailContent.substring(0, 500) + "..." : emailContent);
        
        // Enhanced patterns to handle various "Sent:" formats
        String[] sentPatterns = {
            "(?i)sent:\\s*(.+?)(?=\\n|$)",           // Standard: "Sent: date"
            "(?i)sent on:\\s*(.+?)(?=\\n|$)",       // "Sent on: date"  
            "(?i)sent date:\\s*(.+?)(?=\\n|$)",     // "Sent Date: date"
            "(?i)receiveddate:\\s*(.+?)(?=\\n|$)",  // "ReceivedDate: date"
            "(?i)received date:\\s*(.+?)(?=\\n|$)", // "Received Date: date"
            "(?i)date sent:\\s*(.+?)(?=\\n|$)"      // "Date Sent: date"
        };
        
        String dateString = null;
        String matchedPattern = null;
        
        // Try each sent pattern
        for (String patternStr : sentPatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(emailContent);
            if (matcher.find()) {
                dateString = matcher.group(1).trim();
                matchedPattern = patternStr;
                log.info("📅 Found date using pattern '{}': '{}'", matchedPattern, dateString);
                break;
            }
        }
        
        // Fallback to standard Date header if no Sent patterns found
        if (dateString == null) {
            log.warn("⚠️ No 'Sent:' variations found, trying 'Date:' header");
            Pattern datePattern = Pattern.compile("(?i)date:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
            Matcher dateMatcher = datePattern.matcher(emailContent);
            if (dateMatcher.find()) {
                dateString = dateMatcher.group(1).trim();
                matchedPattern = "date:";
                log.info("📅 Found Date header: '{}'", dateString);
            }
        }
        
        if (dateString != null) {
            LocalDate parsedDate = parseDateString(dateString);
            log.info("📅 Successfully parsed date from '{}': {}", matchedPattern, parsedDate);
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

    // Enhanced project extraction with fuzzy matching
    private Project extractProject(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        log.info("🔍 Extracting project from content: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);
        
        Project bestMatch = null;
        double bestSimilarity = 0.0;
        String bestMatchedTerm = "";
        
        // Check each project with fuzzy matching
        for (Project project : Project.values()) {
            if (project == Project.GENERAL) continue; // Skip GENERAL, use as default
            
            // Get all possible variations for this project
            List<String> projectVariations = getProjectVariations(project);
            
            for (String variation : projectVariations) {
                double similarity = findBestSimilarityInContent(content, variation);
                
                if (similarity > projectSimilarityThreshold && similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = project;
                    bestMatchedTerm = variation;
                    log.info("🎯 Found better match: {} (similarity: {:.2f}) for variation: '{}'", 
                            project.getDisplayName(), similarity, variation);
                }
            }
        }
        
        if (bestMatch != null) {
            log.info("✅ Best project match: {} (similarity: {:.2f}) matched term: '{}'", 
                    bestMatch.getDisplayName(), bestSimilarity, bestMatchedTerm);
            return bestMatch;
        }
        
        log.warn("⚠️ No project match found above threshold {:.2f}, using GENERAL", projectSimilarityThreshold);
        return Project.GENERAL; // Default if no project found
    }
    
    // Get all possible variations for a project (including common typos and formats)
    private List<String> getProjectVariations(Project project) {
        List<String> variations = new ArrayList<>();
        String displayName = project.getDisplayName().toLowerCase();
        variations.add(displayName);
        
        switch (project) {
            case MATERIAL_RECEIPT:
                variations.addAll(Arrays.asList(
                    "material receipt", "materialreceipt", "material reciept", "materialreciept",
                    "material-receipt", "material_receipt", "mat receipt", "matreceipt"
                ));
                break;
            case MY_BUDDY:
                variations.addAll(Arrays.asList(
                    "my buddy", "mybuddy", "my-buddy", "my_buddy", "mybudy", "my budy"
                ));
                break;
            case CK_ALUMNI:
                variations.addAll(Arrays.asList(
                    "ck alumni", "ckalumni", "ck-alumni", "ck_alumni", "c k alumni", "ckpl alumni",
                    "ck alumini", "ckalumini", "ck alumi", "ckalumi"
                ));
                break;
            case HEPL_ALUMNI:
                variations.addAll(Arrays.asList(
                    "hepl alumni", "heplalumni", "hepl-alumni", "hepl_alumni", "h e p l alumni",
                    "hepl alumini", "heplalumini", "hepl alumi", "heplalumi"
                ));
                break;
            case HEPL_PORTAL:
                variations.addAll(Arrays.asList(
                    "hepl portal", "heplportal", "hepl-portal", "hepl_portal", "h e p l portal",
                    "hepl portl", "heplportl", "hepl protal", "heplprotal"
                ));
                break;
            case MMW_MODULE_TICKET_TOOL:
                variations.addAll(Arrays.asList(
                    "mmw module", "mmwmodule", "mmw-module", "mmw_module",
                    "ticket tool", "tickettool", "ticket-tool", "ticket_tool",
                    "mmw ticket tool", "mmwticket tool", "mmw tickettool"
                ));
                break;
            case CK_TRENDS:
                variations.addAll(Arrays.asList(
                    "ck trends", "cktrends", "ck-trends", "ck_trends", "c k trends",
                    "ck trend", "cktrend", "ck trands", "cktrands"
                ));
                break;
            case LIVEWIRE:
                variations.addAll(Arrays.asList(
                    "livewire", "live wire", "live-wire", "live_wire", "livwire", "livewir"
                ));
                break;
            case MEETING_AGENDA:
                variations.addAll(Arrays.asList(
                    "meeting agenda", "meetingagenda", "meeting-agenda", "meeting_agenda",
                    "meet agenda", "meetagenda", "meeting agend", "meetingagend"
                ));
                break;
            case PRO_HIRE:
                variations.addAll(Arrays.asList(
                    "pro hire", "prohire", "pro-hire", "pro_hire", "prohir", "pro hir"
                ));
                break;
            case E_CAPEX:
                variations.addAll(Arrays.asList(
                    "e-capex", "ecapex", "e capex", "e_capex", "e-capx", "ecapx", "e capx"
                ));
                break;
            case SOP:
                variations.addAll(Arrays.asList(
                    "sop", "s o p", "s.o.p", "standard operating procedure", "standard op procedure"
                ));
                break;
            case ASSET_MANAGEMENT:
                variations.addAll(Arrays.asList(
                    "asset management", "assetmanagement", "asset-management", "asset_management",
                    "assert management", "assertmanagement", "asset mgmt", "assetmgmt",
                    "asset managment", "assetmanagment"
                ));
                break;
            case MOULD_MAMP:
                variations.addAll(Arrays.asList(
                    "mould mamp", "mouldmamp", "mould-mamp", "mould_mamp",
                    "mold mamp", "moldmamp", "mould map", "mouldmap"
                ));
                break;
        }
        
        return variations;
    }
    
    // Find the best similarity score for a term within the content
    private double findBestSimilarityInContent(String content, String searchTerm) {
        double bestSimilarity = 0.0;
        
        // Direct substring match (highest priority)
        if (content.contains(searchTerm)) {
            return 1.0; // Perfect match
        }
        
        // Split content into words and check similarity with each word/phrase
        String[] words = content.split("\\s+");
        
        // Check individual words
        for (String word : words) {
            double similarity = jaroWinklerSimilarity.apply(word, searchTerm);
            bestSimilarity = Math.max(bestSimilarity, similarity);
        }
        
        // Check word combinations (for multi-word search terms)
        if (searchTerm.contains(" ")) {
            String[] searchWords = searchTerm.split("\\s+");
            for (int i = 0; i <= words.length - searchWords.length; i++) {
                StringBuilder phrase = new StringBuilder();
                for (int j = 0; j < searchWords.length; j++) {
                    if (j > 0) phrase.append(" ");
                    phrase.append(words[i + j]);
                }
                double similarity = jaroWinklerSimilarity.apply(phrase.toString(), searchTerm);
                bestSimilarity = Math.max(bestSimilarity, similarity);
            }
        }
        
        return bestSimilarity;
    }

    // Enhanced priority extraction with fuzzy matching
    private Priority extractPriority(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        log.info("🔍 Extracting priority from content...");
        
        // Define priority keywords with variations
        List<String> highPriorityTerms = Arrays.asList(
            "critical", "urgent", "high priority", "asap", "emergency", "immediate",
            "critcal", "urgnt", "high priorit", "a s a p", "emergenc", "immedaite"
        );
        
        List<String> lowPriorityTerms = Arrays.asList(
            "low", "minor", "low priority", "not urgent", "when possible",
            "lo", "minr", "low priorit", "not urgnt", "wen possible"
        );
        
        List<String> moderatePriorityTerms = Arrays.asList(
            "medium", "moderate", "normal", "standard", "regular",
            "medim", "moderat", "norml", "standar", "regulr"
        );
        
        // Check for high priority with fuzzy matching
        for (String term : highPriorityTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found HIGH priority match for term: '{}'", term);
                return Priority.HIGH;
            }
        }
        
        // Check for low priority with fuzzy matching
        for (String term : lowPriorityTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found LOW priority match for term: '{}'", term);
                return Priority.LOW;
            }
        }
        
        // Check for moderate priority with fuzzy matching
        for (String term : moderatePriorityTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found MODERATE priority match for term: '{}'", term);
                return Priority.MODERATE;
            }
        }
        
        log.info("⚠️ No priority keywords found, using default MODERATE");
        return Priority.MODERATE; // Default priority
    }

    // Enhanced bug type extraction with fuzzy matching
    private BugType extractBugType(String subject, String body) {
        String content = (subject + " " + body).toLowerCase();
        log.info("🔍 Extracting bug type from content...");
        
        // Define bug type keywords with variations
        List<String> enhancementTerms = Arrays.asList(
            "enhancement", "feature", "improvement", "new feature", "upgrade", "optimize",
            "enhancment", "featur", "improvment", "new featur", "upgrad", "optimiz"
        );
        
        List<String> taskTerms = Arrays.asList(
            "task", "todo", "action item", "work item", "activity", "assignment",
            "tsk", "to do", "action itm", "work itm", "activit", "assignmnt"
        );
        
        List<String> bugTerms = Arrays.asList(
            "bug", "error", "issue", "problem", "defect", "fault", "failure",
            "bg", "eror", "isue", "problm", "defct", "falt", "failur"
        );
        
        // Check for enhancement with fuzzy matching
        for (String term : enhancementTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found ENHANCEMENT match for term: '{}'", term);
                return BugType.ENHANCEMENT;
            }
        }
        
        // Check for task with fuzzy matching
        for (String term : taskTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found TASK match for term: '{}'", term);
                return BugType.TASK;
            }
        }
        
        // Check for bug with fuzzy matching
        for (String term : bugTerms) {
            if (findBestSimilarityInContent(content, term) > prioritySimilarityThreshold) {
                log.info("✅ Found BUG match for term: '{}'", term);
                return BugType.BUG;
            }
        }
        
        log.info("⚠️ No bug type keywords found, using default BUG");
        return BugType.BUG; // Default to bug
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
        
        // No contributor found in database
        log.warn("⚠️ No contributor found in database for emails: {}", emailsInTo);
        return null;
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