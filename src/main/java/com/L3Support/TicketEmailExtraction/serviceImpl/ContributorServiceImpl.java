package com.L3Support.TicketEmailExtraction.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.model.ContributorRequest;
import com.L3Support.TicketEmailExtraction.model.ContributorResponse;
import com.L3Support.TicketEmailExtraction.repository.ContributorRepository;
import com.L3Support.TicketEmailExtraction.service.ContributorService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class ContributorServiceImpl implements ContributorService {

    private final ContributorRepository contributorRepository;

    public ContributorServiceImpl(ContributorRepository contributorRepository) {
        this.contributorRepository = contributorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributorResponse> getAllContributors() {
        log.debug("Fetching all contributors");
        return contributorRepository.findAllByOrderByNameAsc()
                .stream()
                .map(ContributorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributorResponse> getActiveContributors() {
        log.debug("Fetching active contributors");
        return contributorRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(ContributorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContributorResponse> getContributorById(Long id) {
        log.debug("Fetching contributor by ID: {}", id);
        return contributorRepository.findById(id)
                .map(ContributorResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContributorResponse> getContributorByEmail(String email) {
        log.debug("Fetching contributor by email: {}", email);
        return contributorRepository.findByEmail(email)
                .map(ContributorResponse::fromEntity);
    }

    @Override
    public ContributorResponse createContributor(ContributorRequest request) {
        log.debug("Creating new contributor: {}", request.getName());
        
        // Validate email uniqueness
        if (request.getEmail() != null && contributorRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Validate employee ID uniqueness
        if (request.getEmployeeId() != null && contributorRepository.findByEmployeeId(request.getEmployeeId()).isPresent()) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        Contributor contributor = Contributor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .employeeId(request.getEmployeeId())
                .department(request.getDepartment())
                .phone(request.getPhone())
                .active(request.getActive() != null ? request.getActive() : true)
                .notes(request.getNotes())
                .build();

        Contributor saved = contributorRepository.save(contributor);
        log.info("Created contributor with ID: {}", saved.getId());
        
        return ContributorResponse.fromEntity(saved);
    }

    @Override
    public ContributorResponse updateContributor(Long id, ContributorRequest request) {
        log.debug("Updating contributor with ID: {}", id);
        
        Contributor contributor = contributorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contributor not found with ID: " + id));

        // Validate email uniqueness (excluding current contributor)
        if (request.getEmail() != null && contributorRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Validate employee ID uniqueness (excluding current contributor)
        if (request.getEmployeeId() != null && contributorRepository.existsByEmployeeIdAndIdNot(request.getEmployeeId(), id)) {
            throw new IllegalArgumentException("Employee ID already exists: " + request.getEmployeeId());
        }

        // Update fields
        contributor.setName(request.getName());
        contributor.setEmail(request.getEmail());
        contributor.setEmployeeId(request.getEmployeeId());
        contributor.setDepartment(request.getDepartment());
        contributor.setPhone(request.getPhone());
        contributor.setActive(request.getActive() != null ? request.getActive() : contributor.getActive());
        contributor.setNotes(request.getNotes());

        Contributor updated = contributorRepository.save(contributor);
        log.info("Updated contributor with ID: {}", updated.getId());
        
        return ContributorResponse.fromEntity(updated);
    }

    @Override
    public void deleteContributor(Long id) {
        log.debug("Soft deleting contributor with ID: {}", id);
        
        Contributor contributor = contributorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contributor not found with ID: " + id));

        contributor.setActive(false);
        contributorRepository.save(contributor);
        
        log.info("Soft deleted contributor with ID: {}", id);
    }

    @Override
    public void permanentlyDeleteContributor(Long id) {
        log.debug("Permanently deleting contributor with ID: {}", id);
        
        if (!contributorRepository.existsById(id)) {
            throw new IllegalArgumentException("Contributor not found with ID: " + id);
        }

        contributorRepository.deleteById(id);
        log.info("Permanently deleted contributor with ID: {}", id);
    }

    @Override
    public ContributorResponse activateContributor(Long id) {
        log.debug("Activating contributor with ID: {}", id);
        
        Contributor contributor = contributorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contributor not found with ID: " + id));

        contributor.setActive(true);
        Contributor updated = contributorRepository.save(contributor);
        
        log.info("Activated contributor with ID: {}", id);
        return ContributorResponse.fromEntity(updated);
    }

    @Override
    public ContributorResponse deactivateContributor(Long id) {
        log.debug("Deactivating contributor with ID: {}", id);
        
        Contributor contributor = contributorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contributor not found with ID: " + id));

        contributor.setActive(false);
        Contributor updated = contributorRepository.save(contributor);
        
        log.info("Deactivated contributor with ID: {}", id);
        return ContributorResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributorResponse> searchContributorsByName(String name) {
        log.debug("Searching contributors by name: {}", name);
        return contributorRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(ContributorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributorResponse> getContributorsByDepartment(String department) {
        log.debug("Fetching contributors by department: {}", department);
        return contributorRepository.findByDepartmentIgnoreCase(department)
                .stream()
                .map(ContributorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email, Long excludeId) {
        if (email == null) return false;
        if (excludeId == null) {
            return contributorRepository.findByEmail(email).isPresent();
        }
        return contributorRepository.existsByEmailAndIdNot(email, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean employeeIdExists(String employeeId, Long excludeId) {
        if (employeeId == null) return false;
        if (excludeId == null) {
            return contributorRepository.findByEmployeeId(employeeId).isPresent();
        }
        return contributorRepository.existsByEmployeeIdAndIdNot(employeeId, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Contributor> getContributorEntityById(Long id) {
        return contributorRepository.findById(id);
    }
}