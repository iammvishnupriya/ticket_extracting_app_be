package com.L3Support.TicketEmailExtraction.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.L3Support.TicketEmailExtraction.model.Contributor;
import com.L3Support.TicketEmailExtraction.repository.ContributorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService implements CommandLineRunner {

    private final ContributorRepository contributorRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data migration for contributors...");
        initializeDefaultContributors();
        log.info("Data migration completed successfully");
    }

    private void initializeDefaultContributors() {
        // Check if contributors already exist
        if (contributorRepository.count() > 0) {
            log.info("Contributors already exist, skipping initialization");
            return;
        }

        log.info("Initializing default contributors...");

        List<String> defaultContributors = Arrays.asList(
            "Kalpana V",
            "Nandhini P", 
            "Manoj",
            "Afreena",
            "Arun Prasad",
            "Venmani",
            "Athithya",
            "Others"
        );

        for (String name : defaultContributors) {
            try {
                Contributor contributor = Contributor.builder()
                        .name(name)
                        .active(true)
                        .department("L3 Support")
                        .notes("Migrated from static list")
                        .build();

                // Set email for known contributors based on the application.properties pattern
                if (name.equals("Kalpana V")) {
                    contributor.setEmail("kalpana.v@hepl.com");
                } else if (name.equals("Nandhini P")) {
                    contributor.setEmail("nandhini.p@hepl.com");
                } else if (name.equals("Manoj")) {
                    contributor.setEmail("manoj.a@hepl.com");
                } else if (name.equals("Afreena")) {
                    contributor.setEmail("afreena.a@hepl.com");
                } else if (name.equals("Arun Prasad")) {
                    contributor.setEmail("arun.se@hepl.com");
                }

                contributorRepository.save(contributor);
                log.info("Created contributor: {}", name);
            } catch (Exception e) {
                log.error("Error creating contributor: {}", name, e);
            }
        }

        log.info("Successfully initialized {} default contributors", defaultContributors.size());
    }
}