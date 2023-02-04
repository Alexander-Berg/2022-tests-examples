package ru.yandex.qe.dispenser.standalone;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;

import ru.yandex.qe.dispenser.client.v1.DiOAuthToken;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.SpyDispenserFactory;

public class SpyRemoteDispenserFactory implements Supplier<Dispenser> {

    private final SpyDispenserFactory spyDispenserFactory;

    @Inject
    public SpyRemoteDispenserFactory(final ApplicationContext applicationContext) {
        final String host = System.getProperty("dispenser.client.host");
        spyDispenserFactory = new SpyDispenserFactory(host, DiOAuthToken.of("amosov-f"), DispenserConfig.Environment.DEVELOPMENT, () -> applicationContext);
    }

    @Override
    public Dispenser get() {
        return spyDispenserFactory.get();
    }
}
