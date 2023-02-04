package ru.yandex.solomon.alert.notification.channel.juggler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.juggler.client.JugglerClient;
import ru.yandex.juggler.dto.EventStatus;
import ru.yandex.juggler.dto.GetConfigResponse;
import ru.yandex.juggler.dto.JugglerEvent;
import ru.yandex.monlib.metrics.MetricConsumer;
import ru.yandex.monlib.metrics.labels.Labels;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class JugglerClientStub implements JugglerClient {
    private final Deque<JugglerEvent> events = new ConcurrentLinkedDeque<>();

    private volatile EventStatus status = new EventStatus("Ok", 200);

    @Override
    public CompletableFuture<EventStatus> sendEvent(JugglerEvent event) {
        events.add(event);
        return CompletableFuture.completedFuture(status);
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public JugglerEvent getLatestEvent() {
        return events.peekLast();
    }

    public List<JugglerEvent> getEvents() {
        return new ArrayList<>(events);
    }

    @Override
    public void close() {
    }

    @Override
    public void updateTargetConfig(List<GetConfigResponse.Target> config) {
    }

    @Override
    public int estimateCount() {
        return 0;
    }

    @Override
    public void append(long tsMillis, Labels commonLabels, MetricConsumer consumer) {
    }
}
