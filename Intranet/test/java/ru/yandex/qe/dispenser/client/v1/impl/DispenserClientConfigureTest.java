package ru.yandex.qe.dispenser.client.v1.impl;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.client.v1.DiOAuthToken;

public final class DispenserClientConfigureTest {
    @Test
    public void autoDetectEnvironmentByDispenserHost() {
        final DispenserConfig config = new DispenserConfig()
                .setClientId("NIRVANA")
                .setDispenserHost(DispenserConfig.Environment.PRODUCTION.getDispenserHost())
                .setServiceZombieOAuthToken(DiOAuthToken.of(""));
        final URI baseURI = new RemoteDispenserFactory(config).createConfiguredWebClient().getBaseURI();
        Assertions.assertTrue(baseURI.toString().startsWith(DispenserConfig.Environment.PRODUCTION.getDispenserHost()));
    }

    @Test
    public void setEnvironmentIsHigherPriorityThanEnvironmentAutoDetect() {
        final DispenserConfig config = new DispenserConfig()
                .setClientId("NIRVANA")
                .setEnvironment(DispenserConfig.Environment.DEVELOPMENT)
                .setDispenserHost(DispenserConfig.Environment.PRODUCTION.getDispenserHost())
                .setServiceZombieOAuthToken(DiOAuthToken.of(""));
        Assertions.assertEquals(DispenserConfig.Environment.DEVELOPMENT, new RemoteDispenserFactory(config).config.getInfo().getEnvironment());
    }
}