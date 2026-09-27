package com.notification.channel.impl;

import com.notification.channel.NotificationChannel;
import com.notification.channel.dto.ChannelDeliveryResult;
import com.notification.notification.domain.NotificationType;
import com.notification.publisher.event.NotificationEnrichedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookNotificationChannel implements NotificationChannel {

    @Override
    public NotificationType getType() {
        return NotificationType.WEBHOOK;
    }

    @Override
    public ChannelDeliveryResult send(NotificationEnrichedEvent event) {
        log.info("Sending WEBHOOK payload to URL [{}] with content [{}] for tenant [{}]",
                event.getRecipient(), event.getRenderedContent(), event.getTenantId());

        if (event.getRecipient() == null || !event.getRecipient().startsWith("http")) {
            return ChannelDeliveryResult.error("Invalid webhook URL: " + event.getRecipient(), 400);
        }

        // Simulate provider execution
        return ChannelDeliveryResult.ok();
    }
}
