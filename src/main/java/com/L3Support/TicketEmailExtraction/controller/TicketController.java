package com.L3Support.TicketEmailExtraction.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = {
    "http://localhost:3000", 
    "http://localhost:3001", 
    "http://localhost:4200", 
    "http://localhost:5173",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:3001",
    "http://127.0.0.1:4200",
    "http://127.0.0.1:5173"
})
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;

    @Operation(summary = "🔍 Get all tickets")
    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Operation(summary = "📄 Get ticket by ID")
    @GetMapping("/{id}")
    public Ticket getTicketById(@PathVariable Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    @Operation(summary = "📝 Add ticket manually (optional)")
    @PostMapping
    public Ticket createTicket(@RequestBody Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Operation(summary = "✏️ Update existing ticket")
    @PutMapping("/{id}")
    public Ticket updateTicket(@PathVariable Long id, @RequestBody Ticket updatedTicket) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            Ticket existing = ticketOpt.get();

            existing.setTicketSummary(updatedTicket.getTicketSummary());
            existing.setProject(updatedTicket.getProject());
            existing.setIssueDescription(updatedTicket.getIssueDescription());
            existing.setPriority(updatedTicket.getPriority());
            existing.setStatus(updatedTicket.getStatus());
            existing.setReview(updatedTicket.getReview());
            existing.setTicketOwner(updatedTicket.getTicketOwner());
            existing.setContributor(updatedTicket.getContributor());

            return ticketRepository.save(existing);
        }
        return null;
    }

    @Operation(summary = "❌ Delete a ticket")
    @DeleteMapping("/{id}")
    public String deleteTicket(@PathVariable Long id) {
        ticketRepository.deleteById(id);
        return "Ticket deleted successfully";
    }
}
