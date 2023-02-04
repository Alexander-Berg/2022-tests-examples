package ru.yandex.solomon.alert.notification.channel.cloud.sms;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.misc.random.Random2;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.notification.channel.webhook.AsyncHttpClientStub;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class YaCCUrlShortenerTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
        .withTimeout(30, TimeUnit.SECONDS)
        .withLookingForStuckThread(true)
        .build();

    @WillClose
    private AsyncHttpClientStub httpClient;
    private YaCCUrlShortener client;
    private ScheduledExecutorService timer;

    @Before
    public void setUp() throws Exception {
        httpClient = new AsyncHttpClientStub();
        timer = Executors.newSingleThreadScheduledExecutor();
        client = new YaCCUrlShortener("https://api.ya.cc", Duration.ofSeconds(5), httpClient, new MetricRegistry(), timer);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        httpClient.close();
        timer.shutdownNow();
    }

    @Test
    public void sendOk() {
        String url = "https://api.ya.cc/--";
        httpClient.when(request(url)).respond(response("https://ya.ru").withStatusCode(200));
        String shortUrl = client.shorten("https://google.com/").join();
        assertEquals(shortUrl, "https://ya.ru");
    }

    @Test(expected = RuntimeException.class)
    public void sendUnavailable() {
        String url = "https://api.ya.cc/--";
        httpClient.when(request(url)).respond(response("https://ya.ru").withStatusCode(503));
        client.shorten("https://google.com/").join();
    }

    @Test(expected = RuntimeException.class)
    public void garbage() {
        String url = "https://api.ya.cc/--";
        httpClient.when(request(url)).respond(response("<!DOCTYPE html PUBLIC></html>").withStatusCode(200));
        client.shorten("http://google.com/").join();
    }

    @Test(expected = RuntimeException.class)
    public void veryLong() {
        String url = "https://api.ya.cc/--";
        httpClient.when(request(url)).respond(response("https://" + Random2.R.nextString(100)).withStatusCode(200));
        client.shorten("http://google.com/").join();
    }
}
