package com.L3Support.TicketEmailExtraction.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.enums.Project;

@RestController
@RequestMapping("/api/projects")
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
public class ProjectController {

    @GetMapping
    public List<String> getAllProjects() {
        return Arrays.stream(Project.values())
                .map(Project::getDisplayName)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/enum-values")
    public List<Project> getAllProjectEnums() {
        return Arrays.asList(Project.values());
    }
}