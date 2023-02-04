package ru.yandex.solomon.alert.notification.channel.webhook;

import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpResponse;

/**
 * @author Vladimir Gordiychuk
 */
public class ChainExpectation {
    private final Expectation expectation;

    public ChainExpectation(Expectation expectation) {
        this.expectation = expectation;
    }

    public void respond(HttpResponse httpResponse) {
        expectation.thenRespond(httpResponse);
    }
}
