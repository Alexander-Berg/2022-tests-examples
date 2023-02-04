package ru.yandex.realty.module;

import com.google.inject.AbstractModule;

public class RealtySeoModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new RealtyWebConfigModule());
    }
}
