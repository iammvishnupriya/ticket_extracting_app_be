package com.L3Support.TicketEmailExtraction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;
import com.L3Support.TicketEmailExtraction.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    boolean existsByMessageId(String messageId);
    
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.contributor ORDER BY t.id DESC")
    List<Ticket> findAllWithContributors();
    
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.contributor WHERE t.id = :id")
    Optional<Ticket> findByIdWithContributor(@Param("id") Long id);
    
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.contributor WHERE t.status = :status ORDER BY t.id DESC")
    List<Ticket> findByStatusWithContributors(@Param("status") Status status);
    
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.contributor WHERE t.priority = :priority ORDER BY t.id DESC")
    List<Ticket> findByPriorityWithContributors(@Param("priority") Priority priority);
}
