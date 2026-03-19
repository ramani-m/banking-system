package com.ramani.banking.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPreferencesResponse {
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
}
