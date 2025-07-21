package com.L3Support.TicketEmailExtraction.serviceImpl;

import com.L3Support.TicketEmailExtraction.model.ConsolidateResponse;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;
import com.L3Support.TicketEmailExtraction.repository.TicketRepository;
import com.L3Support.TicketEmailExtraction.service.ConsolidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsolidateServiceImpl implements ConsolidateService {
    
    private final TicketRepository ticketRepository;

    @Override
    public List<ConsolidateResponse> getConsolidatedData() {
        List<Ticket> allTickets = ticketRepository.findAll();
        
        // Group tickets by project
        Map<String, List<Ticket>> ticketsByProject = allTickets.stream()
            .collect(Collectors.groupingBy(
                ticket -> ticket.getProject() != null ? ticket.getProject().getDisplayName() : "Unknown"
            ));
        
        // Create consolidated response for each project
        List<ConsolidateResponse> consolidatedData = ticketsByProject.entrySet().stream()
            .map(entry -> {
                String project = entry.getKey();
                List<Ticket> projectTickets = entry.getValue();
                
                long closedCount = projectTickets.stream()
                    .filter(ticket -> isClosedStatus(ticket.getStatus()))
                    .count();
                
                long openCount = projectTickets.stream()
                    .filter(ticket -> !isClosedStatus(ticket.getStatus()))
                    .count();
                
                return new ConsolidateResponse(0, project, closedCount, openCount);
            })
            .collect(Collectors.toList());
        
        // Add serial numbers
        for (int i = 0; i < consolidatedData.size(); i++) {
            consolidatedData.get(i).setSNo(i + 1);
        }
        
        return consolidatedData;
    }
    
    private boolean isClosedStatus(Status status) {
        if (status == null) {
            return false;
        }
        // Consider CLOSED, FIXED, RESOLVED as closed statuses
        return status == Status.CLOSED || status == Status.FIXED || status == Status.RESOLVED;
    }
}