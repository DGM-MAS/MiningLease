package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Data;

@Data
public class EmailRequest {

    private String to;
    private String subject;
    private String body;   // raw body from other services
    private String recipientName;
}