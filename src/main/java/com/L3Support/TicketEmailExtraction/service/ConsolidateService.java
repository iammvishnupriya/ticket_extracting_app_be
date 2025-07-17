package com.L3Support.TicketEmailExtraction.service;

import java.util.List;

import com.L3Support.TicketEmailExtraction.model.ConsolidateResponse;

public interface ConsolidateService {
    List<ConsolidateResponse> getConsolidatedData();
}