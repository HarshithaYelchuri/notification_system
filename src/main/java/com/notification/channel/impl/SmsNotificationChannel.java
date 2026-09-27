package com.notification.channel.impl;

import com.notification.channel.NotificationChannel;
import com.notification.channel.dto.ChannelDeliveryResult;
import com.notification.notification.domain.NotificationType;
import com.notification.publisher.event.NotificationEnrichedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationChannel implements NotificationChannel {

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEnrichedEvent event) {
        log.info("Sending SMS to [{}] with content [{}] for tenant [{}]",
                event.getRecipient(), event.getRenderedContent(), event.getTenantId());

        if (event.getRecipient() == null || event.getRecipient().isBlank()) {
            return ChannelDeliveryResult.error("Recipient phone number is blank", 400);
        }

        // Simulate provider execution
        return ChannelDeliveryResult.ok();
    }
}
