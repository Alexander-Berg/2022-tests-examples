package ru.yandex.arenda.module;

import ru.auto.tests.passport.module.PassportAccountModule;

public class ArendaWebModule extends DefaultModule {

    @Override
    protected void configure() {
        super.configure();
        install(new PassportAccountModule());
    }
}
