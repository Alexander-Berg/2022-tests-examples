package ru.yandex.solomon.alert.notification.channel.cloud.email;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.WillClose;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.cloud.auth.token.TokenProvider;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.cloud.NotifyClientImpl;
import ru.yandex.solomon.alert.notification.channel.cloud.dto.NotifyDto;
import ru.yandex.solomon.alert.notification.channel.webhook.AsyncHttpClientStub;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Vladimir Gordiychuk
 */
public class NotifyClientImplTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
        .withTimeout(30, TimeUnit.SECONDS)
        .withLookingForStuckThread(true)
        .build();

    @WillClose
    private AsyncHttpClientStub httpClient;
    private NotifyClientImpl client;
    private ScheduledExecutorService timer;

    @Before
    public void setUp() throws Exception {
        httpClient = new AsyncHttpClientStub();
        timer = Executors.newSingleThreadScheduledExecutor();
        client = new NotifyClientImpl("http://localhost:8080/notify", httpClient, TokenProvider.of("my-token"), new MetricRegistry(), timer);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        httpClient.close();
        timer.shutdownNow();
    }

    @Test
    public void sendOk() {
        String url = "http://localhost:8080/notify/api/send";
        httpClient.when(request(url)).respond(response().withStatusCode(200));
        var email = new NotifyDto<>("check", null);
        email.receiver = "gordiychuk@yandex-team.ru";
        NotificationStatus status = client.sendEmail(email).join();
        assertEquals(NotificationStatus.Code.SUCCESS, status.getCode());
    }

    @Test
    public void sendUnavailable() {
        String url = "http://localhost:8080/notify/api/send";
        httpClient.when(request(url)).respond(response().withStatusCode(500));
        var email = new NotifyDto<>("check", null);
        email.receiver = "gordiychuk@yandex-team.ru";
        NotificationStatus status = client.sendEmail(email).join();
        assertEquals(NotificationStatus.Code.ERROR_ABLE_TO_RETRY, status.getCode());
    }
}
