package com.L3Support.TicketEmailExtraction.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.repository.ContributorRepository;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketContributorService {

    private final ContributorRepository contributorRepository;
    private final TicketRepository ticketRepository;

    /**
     * Assign contributor to ticket by contributor ID
     */
    public void assignContributorToTicket(Long ticketId, Long contributorId) {
        log.debug("Assigning contributor {} to ticket {}", contributorId, ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        Contributor contributor = contributorRepository.findById(contributorId)
                .orElseThrow(() -> new IllegalArgumentException("Contributor not found with ID: " + contributorId));
        
        ticket.setContributor(contributor);
        ticket.setContributorName(contributor.getName()); // Keep for backward compatibility
        
        ticketRepository.save(ticket);
        log.info("Successfully assigned contributor {} to ticket {}", contributor.getName(), ticketId);
    }

    /**
     * Assign contributor to ticket by contributor name (fallback method)
     */
    public void assignContributorToTicketByName(Long ticketId, String contributorName) {
        log.debug("Assigning contributor by name '{}' to ticket {}", contributorName, ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        // Try to find existing contributor by name
        Optional<Contributor> existingContributor = contributorRepository.findByNameContainingIgnoreCase(contributorName)
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase(contributorName))
                .findFirst();
        
        if (existingContributor.isPresent()) {
            ticket.setContributor(existingContributor.get());
            ticket.setContributorName(existingContributor.get().getName());
            log.info("Found existing contributor: {}", existingContributor.get().getName());
        } else {
            // Create new contributor if not found
            Contributor newContributor = Contributor.builder()
                    .name(contributorName)
                    .active(true)
                    .department("L3 Support")
                    .notes("Auto-created from ticket assignment")
                    .build();
            
            Contributor saved = contributorRepository.save(newContributor);
            ticket.setContributor(saved);
            ticket.setContributorName(saved.getName());
            log.info("Created new contributor: {}", saved.getName());
        }
        
        ticketRepository.save(ticket);
        log.info("Successfully assigned contributor '{}' to ticket {}", contributorName, ticketId);
    }

    /**
     * Remove contributor from ticket
     */
    public void removeContributorFromTicket(Long ticketId) {
        log.debug("Removing contributor from ticket {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        ticket.setContributor(null);
        ticket.setContributorName(null);
        
        ticketRepository.save(ticket);
        log.info("Successfully removed contributor from ticket {}", ticketId);
    }

    /**
     * Get contributor name for ticket (with fallback to contributorName field)
     */
    @Transactional(readOnly = true)
    public String getContributorNameForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        if (ticket.getContributor() != null) {
            return ticket.getContributor().getName();
        }
        
        return ticket.getContributorName(); // Fallback to old field
    }

    /**
     * Migrate existing tickets to use Contributor relationships
     */
    public void migrateTicketContributors() {
        log.info("Starting migration of ticket contributors...");
        
        // Find all tickets that have contributorName but no contributor relationship
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getContributor() == null && ticket.getContributorName() != null && !ticket.getContributorName().trim().isEmpty()) {
                try {
                    assignContributorToTicketByName(ticket.getId(), ticket.getContributorName());
                } catch (Exception e) {
                    log.error("Error migrating contributor for ticket {}: {}", ticket.getId(), e.getMessage());
                }
            }
        });
        
        log.info("Completed migration of ticket contributors");
    }

    // ==================== Multiple Contributors Support (Comma-Separated IDs) ====================

    /**
     * Utility method: Convert comma-separated string to List of Long IDs
     */
    private List<Long> parseContributorIds(String contributorIds) {
        if (!StringUtils.hasText(contributorIds)) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(contributorIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * Utility method: Convert List of Long IDs to comma-separated string
     */
    private String formatContributorIds(List<Long> contributorIds) {
        if (contributorIds == null || contributorIds.isEmpty()) {
            return null;
        }
        
        return contributorIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Add multiple contributors to a ticket using comma-separated IDs
     */
    public void addContributorsToTicket(Long ticketId, List<Long> newContributorIds) {
        log.debug("Adding {} contributors to ticket {}", newContributorIds.size(), ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        // Validate all contributor IDs exist
        for (Long contributorId : newContributorIds) {
            if (!contributorRepository.existsById(contributorId)) {
                throw new IllegalArgumentException("Contributor not found with ID: " + contributorId);
            }
        }
        
        // Get existing contributor IDs
        List<Long> existingIds = parseContributorIds(ticket.getContributorIds());
        
        // Add new IDs that don't already exist
        for (Long newId : newContributorIds) {
            if (!existingIds.contains(newId)) {
                existingIds.add(newId);
                log.info("Added contributor {} to ticket {}", newId, ticketId);
            } else {
                log.debug("Contributor {} already assigned to ticket {}", newId, ticketId);
            }
        }
        
        // Update the ticket with the new comma-separated string
        ticket.setContributorIds(formatContributorIds(existingIds));
        ticketRepository.save(ticket);
    }

    /**
     * Add a single contributor to a ticket
     */
    public void addContributorToTicket(Long ticketId, Long contributorId) {
        addContributorsToTicket(ticketId, Arrays.asList(contributorId));
    }

    /**
     * Remove a specific contributor from a ticket
     */
    public void removeContributorFromTicket(Long ticketId, Long contributorId) {
        log.debug("Removing contributor {} from ticket {}", contributorId, ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        List<Long> existingIds = parseContributorIds(ticket.getContributorIds());
        
        if (existingIds.remove(contributorId)) {
            ticket.setContributorIds(formatContributorIds(existingIds));
            ticketRepository.save(ticket);
            log.info("Successfully removed contributor {} from ticket {}", contributorId, ticketId);
        } else {
            log.warn("Contributor {} was not assigned to ticket {}", contributorId, ticketId);
        }
    }

    /**
     * Remove all contributors from a ticket
     */
    public void removeAllContributorsFromTicket(Long ticketId) {
        log.debug("Removing all contributors from ticket {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        List<Long> existingIds = parseContributorIds(ticket.getContributorIds());
        int count = existingIds.size();
        
        ticket.setContributorIds(null);
        ticketRepository.save(ticket);
        
        log.info("Successfully removed {} contributors from ticket {}", count, ticketId);
    }

    /**
     * Replace all contributors for a ticket with new ones
     */
    public void replaceContributorsForTicket(Long ticketId, List<Long> contributorIds) {
        log.debug("Replacing contributors for ticket {} with {} new contributors", ticketId, contributorIds.size());
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        // Validate all contributor IDs exist
        for (Long contributorId : contributorIds) {
            if (!contributorRepository.existsById(contributorId)) {
                throw new IllegalArgumentException("Contributor not found with ID: " + contributorId);
            }
        }
        
        ticket.setContributorIds(formatContributorIds(contributorIds));
        ticketRepository.save(ticket);
        
        log.info("Successfully replaced contributors for ticket {} with {} new contributors", ticketId, contributorIds.size());
    }

    /**
     * Get all contributors for a ticket
     */
    @Transactional(readOnly = true)
    public List<Contributor> getContributorsForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        List<Long> contributorIds = parseContributorIds(ticket.getContributorIds());
        
        return contributorIds.stream()
                .map(id -> contributorRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Get all tickets for a contributor
     */
    @Transactional(readOnly = true)
    public List<Ticket> getTicketsForContributor(Long contributorId) {
        // Find all tickets where contributorIds contains the given contributorId
        return ticketRepository.findAll().stream()
                .filter(ticket -> {
                    List<Long> ids = parseContributorIds(ticket.getContributorIds());
                    return ids.contains(contributorId);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get contributor names for a ticket (backward compatibility)
     */
    @Transactional(readOnly = true)
    public List<String> getContributorNamesForTicket(Long ticketId) {
        List<Contributor> contributors = getContributorsForTicket(ticketId);
        return contributors.stream()
                .map(Contributor::getName)
                .collect(Collectors.toList());
    }

    /**
     * Check if a contributor is assigned to a ticket
     */
    @Transactional(readOnly = true)
    public boolean isContributorAssignedToTicket(Long ticketId, Long contributorId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return false;
        }
        
        List<Long> contributorIds = parseContributorIds(ticket.getContributorIds());
        return contributorIds.contains(contributorId);
    }

    /**
     * Count contributors for a ticket
     */
    @Transactional(readOnly = true)
    public long countContributorsForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return 0;
        }
        
        List<Long> contributorIds = parseContributorIds(ticket.getContributorIds());
        return contributorIds.size();
    }

    /**
     * Count tickets for a contributor
     */
    @Transactional(readOnly = true)
    public long countTicketsForContributor(Long contributorId) {
        return ticketRepository.findAll().stream()
                .filter(ticket -> {
                    List<Long> ids = parseContributorIds(ticket.getContributorIds());
                    return ids.contains(contributorId);
                })
                .count();
    }

    /**
     * Get contributor IDs as a list for a ticket
     */
    @Transactional(readOnly = true)
    public List<Long> getContributorIdsForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + ticketId));
        
        return parseContributorIds(ticket.getContributorIds());
    }
}