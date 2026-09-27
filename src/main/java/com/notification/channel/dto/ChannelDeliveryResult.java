package com.notification.channel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDeliveryResult {

    private boolean success;
    private String errorMessage;
    private int statusCode;

    public static ChannelDeliveryResult ok() {
        return ChannelDeliveryResult.builder()
                .success(true)
                .statusCode(200)
                .build();
    }

    public static ChannelDeliveryResult error(String errorMessage, int statusCode) {
        return ChannelDeliveryResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .statusCode(statusCode)
                .build();
    }
}
