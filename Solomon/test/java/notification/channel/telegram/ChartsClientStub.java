package ru.yandex.solomon.alert.notification.channel.telegram;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import ru.yandex.bolts.collection.Try;
import ru.yandex.solomon.alert.charts.ChartsClient;
import ru.yandex.solomon.alert.notification.channel.AlertApiKey;


/**
 * @author alexlovkov
 **/
public class ChartsClientStub implements ChartsClient {

    private Try<byte[]> predefinedResult = Try.success(new byte[] {-119,  80,  78,  71, 13,  10,  26,  10});

    public void predefineResult(Try<byte[]> result) {
        this.predefinedResult = result;
    }

    @Override
    public CompletableFuture<byte[]> getScreenshot(AlertApiKey alertApiKey, Instant time) {
        return predefinedResult.toCompletedFuture();
    }
}
