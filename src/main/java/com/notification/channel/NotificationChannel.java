package com.notification.channel;

import com.notification.channel.dto.ChannelDeliveryResult;
import com.notification.notification.domain.NotificationType;
import com.notification.publisher.event.NotificationEnrichedEvent;

public interface NotificationChannel {

    NotificationType getType();

    ChannelDeliveryResult send(NotificationEnrichedEvent event);
}
