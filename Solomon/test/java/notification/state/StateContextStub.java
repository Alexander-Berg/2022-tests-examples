package ru.yandex.solomon.alert.notification.state;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.ChannelMetrics;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.NotificationServiceMetrics;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;

/**
 * @author Vladimir Gordiychuk
 */
public class StateContextStub implements StateContext {
    private final NotificationChannel channel;
    private final ChannelConfig channelConfig;
    private final ScheduledExecutorService executorService;
    private final RetryOptions options;
    private final AtomicReference<NotificationChannelState> state = new AtomicReference<>();
    private final NotificationServiceMetrics metrics = new NotificationServiceMetrics();

    public StateContextStub(NotificationChannel channel, ChannelConfig channelConfig, ScheduledExecutorService executorService, RetryOptions options) {
        this.channel = channel;
        this.channelConfig = channelConfig;
        this.executorService = executorService;
        this.options = options;
    }

    @Override
    public NotificationChannel getChannel() {
        return channel;
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public RetryOptions getRetryOptions() {
        return options;
    }

    @Override
    public ChannelMetrics getMetrics() {
        return metrics.getChannelMetrics(channel.getType());
    }

    @Override
    public boolean tryChangeState(NotificationChannelState current, NotificationChannelState next) {
        return this.state.compareAndSet(current, next);
    }

    @Override
    public NotificationChannelState getCurrentChannelState() {
        return this.state.get();
    }

    @Override
    public DispatchRule getDispatchRule() {
        return channel.getDispatchRule(channelConfig);
    }

    public void setState(NotificationChannelState state) {
        this.state.set(state);
    }

    public NotificationStatus syncSend(Event message) {
        return state.get().process(message).join();
    }
}
