package com.L3Support.TicketEmailExtraction.service;

import java.util.List;
import java.util.Optional;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.ContributorRequest;
import com.L3Support.TicketEmailExtraction.model.ContributorResponse;

public interface ContributorService {

    /**
     * Get all contributors
     */
    List<ContributorResponse> getAllContributors();

    /**
     * Get all active contributors
     */
    List<ContributorResponse> getActiveContributors();

    /**
     * Get contributor by ID
     */
    Optional<ContributorResponse> getContributorById(Long id);

    /**
     * Get contributor by email
     */
    Optional<ContributorResponse> getContributorByEmail(String email);

    /**
     * Create new contributor
     */
    ContributorResponse createContributor(ContributorRequest request);

    /**
     * Update existing contributor
     */
    ContributorResponse updateContributor(Long id, ContributorRequest request);

    /**
     * Delete contributor (soft delete - set active to false)
     */
    void deleteContributor(Long id);

    /**
     * Permanently delete contributor
     */
    void permanentlyDeleteContributor(Long id);

    /**
     * Activate contributor
     */
    ContributorResponse activateContributor(Long id);

    /**
     * Deactivate contributor
     */
    ContributorResponse deactivateContributor(Long id);

    /**
     * Search contributors by name
     */
    List<ContributorResponse> searchContributorsByName(String name);

    /**
     * Get contributors by department
     */
    List<ContributorResponse> getContributorsByDepartment(String department);

    /**
     * Check if email exists (for validation)
     */
    boolean emailExists(String email, Long excludeId);

    /**
     * Check if employee ID exists (for validation)
     */
    boolean employeeIdExists(String employeeId, Long excludeId);

    /**
     * Get contributor entity by ID (for internal use)
     */
    Optional<Contributor> getContributorEntityById(Long id);
}