package com.L3Support.TicketEmailExtraction.service;

import java.util.List;
import java.util.Optional;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.model.TicketResponse;

public interface TicketService {
    
    List<TicketResponse> getAllTickets();
    
    Optional<TicketResponse> getTicketById(Long id);
    
    TicketResponse createTicket(Ticket ticket);
    
    TicketResponse updateTicket(Long id, Ticket updatedTicket);
    
    void deleteTicket(Long id);
    
    List<TicketResponse> getTicketsByStatus(String status);
    
    List<TicketResponse> getTicketsByPriority(String priority);
}