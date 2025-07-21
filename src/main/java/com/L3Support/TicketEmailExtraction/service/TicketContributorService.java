package com.L3Support.TicketEmailExtraction.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}