package com.L3Support.TicketEmailExtraction.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;
import com.L3Support.TicketEmailExtraction.utils.CommonConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileBasedEmailService {

    private final TicketRepository ticketRepository;
    private final TicketParserService ticketParserService;

    @Value("${app.email.input.folder:./emails}")
    private String inputFolderPath;

    @Value("${app.email.processed.folder:./emails/processed}")
    private String processedFolderPath;

    @Value("${app.email.error.folder:./emails/error}")
    private String errorFolderPath;

    private final List<String> allowedProjects = CommonConstant.L3_ALLOWED_PROJECTS;

    @Value("${app.processing.auto.move.files:true}")
    private boolean autoMoveFiles;

    @Value("${app.processing.create.backup:true}")
    private boolean createBackup;

    public void processAllEmails() {
        try {
            log.info("🔄 Starting L3 support email processing...");
            
            // Initialize directories
            initializeDirectories();
            
            Path inputPath = Paths.get(inputFolderPath);
            if (!Files.exists(inputPath)) {
                log.error("❌ Input folder does not exist: {}", inputFolderPath);
                return;
            }

            // Process all email files
            try (Stream<Path> files = Files.list(inputPath)) {
                List<Path> emailFiles = files.filter(Files::isRegularFile)
                     .filter(this::isValidEmailFile)
                     .toList();

                log.info("📧 Found {} email files to process", emailFiles.size());
                
                int processedCount = 0;
                int errorCount = 0;
                
                for (Path file : emailFiles) {
                    try {
                        if (processEmailFile(file)) {
                            processedCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (Exception e) {
                        log.error("❌ Error processing file {}: {}", file.getFileName(), e.getMessage());
                        moveToErrorFolder(file, e.getMessage());
                        errorCount++;
                    }
                }

                log.info("📊 Processing complete. Success: {}, Errors: {}", processedCount, errorCount);
            }

        } catch (IOException e) {
            log.error("❌ Error during email processing: {}", e.getMessage());
            throw new RuntimeException("Email processing failed", e);
        }
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(inputFolderPath));
            Files.createDirectories(Paths.get(processedFolderPath));
            Files.createDirectories(Paths.get(errorFolderPath));
            log.debug("📁 Directories initialized");
        } catch (IOException e) {
            log.error("❌ Failed to create directories: {}", e.getMessage());
        }
    }

    private boolean isValidEmailFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".txt") || fileName.endsWith(".eml") || fileName.endsWith(".msg");
    }

    private boolean processEmailFile(Path filePath) {
        try {
            log.info("📄 Processing file: {}", filePath.getFileName());
            
            String content = Files.readString(filePath);
            
            // Check if file has been processed
            String messageId = filePath.getFileName().toString();
            if (ticketRepository.existsByMessageId(messageId)) {
                log.debug("⏭️ File already processed: {}", messageId);
                if (autoMoveFiles) {
                    moveToProcessedFolder(filePath);
                }
                return false;
            }

            // Validate project
            if (!isValidProject(content)) {
                log.debug("⏭️ No valid project found in file: {}", messageId);
                return false;
            }

            // Parse and save ticket
            Ticket ticket = ticketParserService.parseEmailToTicket(content);
            if (ticket != null) {
                ticket.setMessageId(messageId);
                ticketRepository.save(ticket);
                log.info("✅ Saved ticket: {} (ID: {})", ticket.getTicketSummary(), ticket.getId());
                
                // Move processed file
                if (autoMoveFiles) {
                    moveToProcessedFolder(filePath);
                }
                return true;
            } else {
                log.warn("⚠️ Failed to parse ticket from file: {}", messageId);
                return false;
            }

        } catch (IOException e) {
            log.error("❌ Error reading file {}: {}", filePath.getFileName(), e.getMessage());
            return false;
        }
    }

    private boolean isValidProject(String content) {
        return allowedProjects.stream()
            .anyMatch(project -> content.contains(project) || content.contains("Project/Product: " + project));
    }

    private void moveToProcessedFolder(Path filePath) {
        try {
            Path processedPath = Paths.get(processedFolderPath);
            Files.createDirectories(processedPath);
            
            Path targetPath = processedPath.resolve(filePath.getFileName());
            
            // Add timestamp if file already exists
            if (Files.exists(targetPath)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = filePath.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.'));
                targetPath = processedPath.resolve(baseName + "_" + timestamp + extension);
            }
            
            Files.move(filePath, targetPath);
            log.debug("📁 Moved to processed: {}", targetPath.getFileName());
        } catch (IOException e) {
            log.warn("⚠️ Could not move processed file: {}", e.getMessage());
        }
    }

    private void moveToErrorFolder(Path filePath, String errorMessage) {
        try {
            Path errorPath = Paths.get(errorFolderPath);
            Files.createDirectories(errorPath);
            
            // Create error info file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = filePath.getFileName().toString();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            
            Path targetPath = errorPath.resolve(baseName + "_" + timestamp + extension);
            Path errorInfoPath = errorPath.resolve(baseName + "_" + timestamp + "_error.txt");
            
            Files.move(filePath, targetPath);
            Files.writeString(errorInfoPath, "Error: " + errorMessage + "\nTimestamp: " + timestamp);
            
            log.debug("📁 Moved to error folder: {}", targetPath.getFileName());
        } catch (IOException e) {
            log.warn("⚠️ Could not move error file: {}", e.getMessage());
        }
    }

    public ProcessingStats getProcessingStats() {
        try {
            long pendingCount = countFilesInFolder(inputFolderPath);
            long processedCount = countFilesInFolder(processedFolderPath);
            long errorCount = countFilesInFolder(errorFolderPath);
            
            return new ProcessingStats(pendingCount, processedCount, errorCount);
        } catch (IOException e) {
            log.error("❌ Error getting processing stats: {}", e.getMessage());
            return new ProcessingStats(0, 0, 0);
        }
    }

    private long countFilesInFolder(String folderPath) throws IOException {
        Path path = Paths.get(folderPath);
        if (!Files.exists(path)) {
            return 0;
        }
        
        try (Stream<Path> files = Files.list(path)) {
            return files.filter(Files::isRegularFile)
                       .filter(this::isValidEmailFile)
                       .count();
        }
    }

    public static class ProcessingStats {
        private final long pendingCount;
        private final long processedCount;
        private final long errorCount;

        public ProcessingStats(long pendingCount, long processedCount, long errorCount) {
            this.pendingCount = pendingCount;
            this.processedCount = processedCount;
            this.errorCount = errorCount;
        }

        public long getPendingCount() { return pendingCount; }
        public long getProcessedCount() { return processedCount; }
        public long getErrorCount() { return errorCount; }
    }
}