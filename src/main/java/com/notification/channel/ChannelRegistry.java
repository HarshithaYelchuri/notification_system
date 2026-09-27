package com.notification.channel;

import com.notification.common.exception.ValidationException;
import com.notification.notification.domain.NotificationType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelRegistry {

    private final Map<NotificationType, NotificationChannel> channelMap = new EnumMap<>(NotificationType.class);

    public ChannelRegistry(List<NotificationChannel> channels) {
        for (NotificationChannel channel : channels) {
            channelMap.put(channel.getType(), channel);
        }
    }

    public NotificationChannel getChannel(NotificationType type) {
        NotificationChannel channel = channelMap.get(type);
        if (channel == null) {
            throw new ValidationException("No channel worker registered for type: " + type);
        }
        return channel;
    }
}
