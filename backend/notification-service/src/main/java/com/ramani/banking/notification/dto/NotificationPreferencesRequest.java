package com.ramani.banking.notification.dto;

import lombok.Data;

@Data
public class NotificationPreferencesRequest {
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
}
