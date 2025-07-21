package com.L3Support.TicketEmailExtraction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.L3Support.TicketEmailExtraction.model.Contributor;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {

    /**
     * Find contributor by email
     */
    Optional<Contributor> findByEmail(String email);

    /**
     * Find contributor by employee ID
     */
    Optional<Contributor> findByEmployeeId(String employeeId);

    /**
     * Find all active contributors
     */
    List<Contributor> findByActiveTrue();

    /**
     * Find all inactive contributors
     */
    List<Contributor> findByActiveFalse();

    /**
     * Find contributors by department
     */
    List<Contributor> findByDepartmentIgnoreCase(String department);

    /**
     * Search contributors by name (case-insensitive)
     */
    @Query("SELECT c FROM Contributor c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Contributor> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Check if email exists (excluding specific ID for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM Contributor c WHERE c.email = :email AND c.id != :id")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);

    /**
     * Check if employee ID exists (excluding specific ID for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM Contributor c WHERE c.employeeId = :employeeId AND c.id != :id")
    boolean existsByEmployeeIdAndIdNot(@Param("employeeId") String employeeId, @Param("id") Long id);

    /**
     * Get contributors ordered by name
     */
    List<Contributor> findAllByOrderByNameAsc();

    /**
     * Get active contributors ordered by name
     */
    List<Contributor> findByActiveTrueOrderByNameAsc();
}