package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.exception.BadRequestException;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link NotificationSenderRouter}.
 * Auto-discovers all {@link NotificationSender} beans and routes by channel.
 * <p>
 * Adding new channels only requires registering a new {@link NotificationSender} bean.
 */
@Component
public class NotificationSenderRouterImpl implements NotificationSenderRouter {

    private final Map<NotificationChannel, NotificationSender> senderMap;

    public NotificationSenderRouterImpl(List<NotificationSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::supportedChannel, Function.identity()));
    }

    @Override
    public void route(Notification notification, boolean simulateFailure) {
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            throw new BadRequestException(
                    "No sender configured for channel: " + notification.getChannel());
        }
        sender.send(notification, simulateFailure);
    }
}
