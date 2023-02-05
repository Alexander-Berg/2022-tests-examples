package ru.yandex.market;

import androidx.annotation.NonNull;
import ru.yandex.market.application.ApplicationDelegate;
import ru.yandex.market.application.MarketApplication;

public class NoOpMarketApplication extends MarketApplication {

    @Override
    protected void setupEnvironment() {
        // no-op
    }

    @NonNull
    @Override
    protected ApplicationDelegate createMainDelegate() {
        return new NoOpMarketApplicationDelegate(this);
    }
}
