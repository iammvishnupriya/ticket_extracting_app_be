package com.L3Support.TicketEmailExtraction.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.L3Support.TicketEmailExtraction.model.ConsolidateResponse;
import com.L3Support.TicketEmailExtraction.service.ConsolidateService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/consolidate")
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
@RequiredArgsConstructor
public class ConsolidateController {

    private final ConsolidateService consolidateService;

    @Operation(summary = "📊 Get consolidated bug tracking data by project")
    @GetMapping
    public List<ConsolidateResponse> getConsolidatedData() {
        return consolidateService.getConsolidatedData();
    }
}