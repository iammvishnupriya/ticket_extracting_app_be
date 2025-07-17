package com.L3Support.TicketEmailExtraction.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.enums.Project;
import com.L3Support.TicketEmailExtraction.model.BugType;
import com.L3Support.TicketEmailExtraction.model.Priority;
import com.L3Support.TicketEmailExtraction.model.Status;

@RestController
@RequestMapping("/api/enums")
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
public class EnumController {

    @GetMapping("/all")
    public Map<String, List<String>> getAllEnums() {
        Map<String, List<String>> enums = new HashMap<>();
        
        enums.put("projects", Arrays.stream(Project.values())
                .map(Project::getDisplayName)
                .collect(Collectors.toList()));
        
        enums.put("priorities", Arrays.stream(Priority.values())
                .map(Priority::name)
                .collect(Collectors.toList()));
        
        enums.put("bugTypes", Arrays.stream(BugType.values())
                .map(BugType::name)
                .collect(Collectors.toList()));
        
        enums.put("statuses", Arrays.stream(Status.values())
                .map(Status::name)
                .collect(Collectors.toList()));
        
        return enums;
    }
    
    @GetMapping("/projects")
    public List<String> getProjects() {
        return Arrays.stream(Project.values())
                .map(Project::getDisplayName)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/priorities")
    public List<String> getPriorities() {
        return Arrays.stream(Priority.values())
                .map(Priority::name)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/bug-types")
    public List<String> getBugTypes() {
        return Arrays.stream(BugType.values())
                .map(BugType::name)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/statuses")
    public List<String> getStatuses() {
        return Arrays.stream(Status.values())
                .map(Status::name)
                .collect(Collectors.toList());
    }
}