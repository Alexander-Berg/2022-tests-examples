package ru.yandex.qe.dispenser.client.v1.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import ru.yandex.qe.bus.test.BusEndpointsSpy;
import ru.yandex.qe.dispenser.client.v1.DiOAuthToken;

public final class SpyDispenserFactory extends DispenserFactoryImpl {
    private static final String SPY_ADDRESS = "local:/";

    @Nullable
    private final String host;
    @NotNull
    private final Supplier<ApplicationContext> context;

    public SpyDispenserFactory(@Nullable final String host,
                               @NotNull final DiOAuthToken token,
                               @NotNull final DispenserConfig.Environment env,
                               @NotNull final Supplier<ApplicationContext> context) {
        super(config(host, token, env));
        this.host = host;
        this.context = context;
    }

    @NotNull
    private static DispenserConfig config(@Nullable final String host,
                                          @NotNull final DiOAuthToken token,
                                          @NotNull final DispenserConfig.Environment env) {
        return new DispenserConfig()
                .setClientId("DISPENSER")
                .setEnvironment(env)
                .setDispenserHost(host != null ? host : SPY_ADDRESS)
                .setServiceZombieOAuthToken(token);
    }

    @Override
    @NotNull
    public WebClient createConfiguredWebClient() {
        final WebClient client = super.createConfiguredWebClient();
        if (host == null) {
            final ClientConfiguration clientConfig = WebClient.getConfig(client);
            clientConfig.setBus(((BusEndpointsSpy) context.get().getBean("busEndpointsSpy")).getSpyBus());
            clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        }
        return client;
    }

    @NotNull
    @Override
    List<?> getProviders() {
        final List<Object> providers = new ArrayList<>(super.getProviders());
        providers.addAll((List<?>) context.get().getBean("busProviders"));
        return providers;
    }

    @NotNull
    @Override
    public WebClient createUnconfiguredClient(@NotNull final String address) {
        return new SpyWebClient(address);
    }
}

