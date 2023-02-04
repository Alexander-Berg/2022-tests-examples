package ru.auto.tests.realtyapi;

import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.rent.AbstractHandlerTest;

public class RealtyTestApiModule extends RealtyApiModule {

    @Override
    protected void configure() {
        super.configure();
        requestStaticInjection(AbstractHandlerTest.class);
    }
}
