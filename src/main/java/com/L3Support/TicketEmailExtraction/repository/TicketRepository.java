package com.L3Support.TicketEmailExtraction.repository;

import com.L3Support.TicketEmailExtraction.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByMessageId(String messageId);
}
