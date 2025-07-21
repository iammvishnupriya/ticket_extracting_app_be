package com.L3Support.TicketEmailExtraction.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.model.TicketResponse;
import com.L3Support.TicketEmailExtraction.repository.ContributorRepository;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;
import com.L3Support.TicketEmailExtraction.service.TicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {
    
    private final TicketRepository ticketRepository;
    private final ContributorRepository contributorRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        log.debug("Fetching all tickets");
        try {
            List<Ticket> tickets = ticketRepository.findAllWithContributors();
            return tickets.stream()
                    .map(TicketResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all tickets", e);
            throw new RuntimeException("Failed to fetch tickets", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TicketResponse> getTicketById(Long id) {
        log.debug("Fetching ticket by ID: {}", id);
        try {
            return ticketRepository.findByIdWithContributor(id)
                    .map(TicketResponse::fromEntity);
        } catch (Exception e) {
            log.error("Error fetching ticket by ID: {}", id, e);
            throw new RuntimeException("Failed to fetch ticket by ID: " + id, e);
        }
    }
    
    @Override
    public TicketResponse createTicket(Ticket ticket) {
        log.debug("Creating new ticket: {}", ticket.getTicketSummary());
        try {
            Ticket saved = ticketRepository.save(ticket);
            log.info("Created ticket with ID: {}", saved.getId());
            return TicketResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error creating ticket", e);
            throw new RuntimeException("Failed to create ticket", e);
        }
    }
    
    @Override
    public TicketResponse updateTicket(Long id, Ticket updatedTicket) {
        log.debug("Updating ticket with ID: {}", id);
        try {
            Ticket existingTicket = ticketRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found with ID: " + id));
            
            // Update fields
            existingTicket.setTicketSummary(updatedTicket.getTicketSummary());
            existingTicket.setProject(updatedTicket.getProject());
            existingTicket.setIssueDescription(updatedTicket.getIssueDescription());
            existingTicket.setReceivedDate(updatedTicket.getReceivedDate());
            existingTicket.setPriority(updatedTicket.getPriority());
            existingTicket.setStatus(updatedTicket.getStatus());
            existingTicket.setReview(updatedTicket.getReview());
            existingTicket.setTicketOwner(updatedTicket.getTicketOwner());
            
            // Handle contributor updates
            updateContributor(existingTicket, updatedTicket);
            
            existingTicket.setBugType(updatedTicket.getBugType());
            existingTicket.setImpact(updatedTicket.getImpact());
            existingTicket.setContact(updatedTicket.getContact());
            existingTicket.setEmployeeId(updatedTicket.getEmployeeId());
            existingTicket.setEmployeeName(updatedTicket.getEmployeeName());
            
            Ticket saved = ticketRepository.save(existingTicket);
            log.info("Updated ticket with ID: {}", saved.getId());
            return TicketResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error updating ticket with ID: {}", id, e);
            throw new RuntimeException("Failed to update ticket with ID: " + id, e);
        }
    }
    
    @Override
    public void deleteTicket(Long id) {
        log.debug("Deleting ticket with ID: {}", id);
        try {
            if (!ticketRepository.existsById(id)) {
                throw new IllegalArgumentException("Ticket not found with ID: " + id);
            }
            ticketRepository.deleteById(id);
            log.info("Deleted ticket with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting ticket with ID: {}", id, e);
            throw new RuntimeException("Failed to delete ticket with ID: " + id, e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByStatus(String status) {
        log.debug("Fetching tickets by status: {}", status);
        try {
            Status statusEnum = Status.valueOf(status.toUpperCase());
            List<Ticket> tickets = ticketRepository.findByStatusWithContributors(statusEnum);
            return tickets.stream()
                    .map(TicketResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching tickets by status: {}", status, e);
            throw new RuntimeException("Failed to fetch tickets by status: " + status, e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByPriority(String priority) {
        log.debug("Fetching tickets by priority: {}", priority);
        try {
            Priority priorityEnum = Priority.valueOf(priority.toUpperCase());
            List<Ticket> tickets = ticketRepository.findByPriorityWithContributors(priorityEnum);
            return tickets.stream()
                    .map(TicketResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching tickets by priority: {}", priority, e);
            throw new RuntimeException("Failed to fetch tickets by priority: " + priority, e);
        }
    }
    
    /**
     * Helper method to handle contributor updates with multiple input formats
     */
    private void updateContributor(Ticket existingTicket, Ticket updatedTicket) {
        // Priority 1: contributorId is provided (most common from frontend)
        if (updatedTicket.getContributorId() != null) {
            if (updatedTicket.getContributorId() == 0 || updatedTicket.getContributorId() < 0) {
                // Special case: 0 or negative ID means remove contributor
                existingTicket.setContributor(null);
                existingTicket.setContributorName(null);
                log.debug("Removed contributor from ticket");
            } else {
                // Find and assign contributor by ID
                Optional<Contributor> contributor = contributorRepository.findById(updatedTicket.getContributorId());
                if (contributor.isPresent()) {
                    existingTicket.setContributor(contributor.get());
                    existingTicket.setContributorName(contributor.get().getName());
                    log.debug("Updated contributor to: {} (ID: {})", contributor.get().getName(), updatedTicket.getContributorId());
                } else {
                    log.warn("Contributor not found with ID: {}. Keeping existing contributor.", updatedTicket.getContributorId());
                }
            }
        }
        // Priority 2: Full contributor object is provided
        else if (updatedTicket.getContributor() != null) {
            existingTicket.setContributor(updatedTicket.getContributor());
            existingTicket.setContributorName(updatedTicket.getContributor().getName());
            log.debug("Updated contributor to: {}", updatedTicket.getContributor().getName());
        }
        // Priority 3: Only contributor name is provided (fallback)
        else if (updatedTicket.getContributorName() != null && !updatedTicket.getContributorName().trim().isEmpty()) {
            // Try to find existing contributor by name
            Optional<Contributor> contributor = contributorRepository.findByNameContainingIgnoreCase(updatedTicket.getContributorName())
                    .stream()
                    .filter(c -> c.getName().equalsIgnoreCase(updatedTicket.getContributorName().trim()))
                    .findFirst();
            
            if (contributor.isPresent()) {
                existingTicket.setContributor(contributor.get());
                existingTicket.setContributorName(contributor.get().getName());
                log.debug("Updated contributor to existing: {}", contributor.get().getName());
            } else {
                // Just update the name field for now (could create new contributor if needed)
                existingTicket.setContributorName(updatedTicket.getContributorName().trim());
                log.debug("Updated contributor name to: {}", updatedTicket.getContributorName());
            }
        }
        // Priority 4: Explicit null values (remove contributor)
        else if (updatedTicket.getContributorName() != null && updatedTicket.getContributorName().trim().isEmpty()) {
            existingTicket.setContributor(null);
            existingTicket.setContributorName(null);
            log.debug("Explicitly removed contributor (empty name provided)");
        }
        // Otherwise: preserve existing values (no contributor update requested)
    }
}