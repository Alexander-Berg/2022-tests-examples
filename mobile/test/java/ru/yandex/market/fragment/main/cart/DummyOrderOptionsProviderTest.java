package ru.yandex.market.fragment.main.cart;

import org.junit.Test;

import ru.yandex.market.clean.data.provider.DummyOrderOptionsProvider;

public class DummyOrderOptionsProviderTest {

    @Test
    public void testCreateDummyOrderOptionsWithoutExceptions() {
        new DummyOrderOptionsProvider().getDummyOrderOptions();
    }
}