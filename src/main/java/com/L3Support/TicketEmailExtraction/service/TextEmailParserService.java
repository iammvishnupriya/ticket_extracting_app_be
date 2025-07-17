package com.L3Support.TicketEmailExtraction.service;

import com.L3Support.TicketEmailExtraction.model.Ticket;

public interface TextEmailParserService {
    Ticket parseEmailToTicket(String emailContent);
}