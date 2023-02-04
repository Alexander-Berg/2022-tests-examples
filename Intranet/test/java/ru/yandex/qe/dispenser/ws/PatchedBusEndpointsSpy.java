package ru.yandex.qe.dispenser.ws;

import java.util.Objects;

import org.apache.cxf.Bus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;

import ru.yandex.qe.bus.factories.BusEndpointBuilder;
import ru.yandex.qe.bus.test.BusEndpointsSpy;

public final class PatchedBusEndpointsSpy extends BusEndpointsSpy {
    @Nullable
    private Bus spyBus;

    @NotNull
    @Override
    public Object postProcessAfterInitialization(@NotNull final Object bean, @NotNull final String beanName) throws BeansException {
        if (bean instanceof BusEndpointBuilder) {
            spyBus = Mockito.spy(((BusEndpointBuilder) bean).getBus());
        }
        return bean;
    }

    @NotNull
    @Override
    public Bus getSpyBus() {
        return Objects.requireNonNull(spyBus, "No BusEndpointBuilder in context!");
    }

    @Override
    public void resetMocks() {
        Mockito.reset(spyBus);
    }
}
