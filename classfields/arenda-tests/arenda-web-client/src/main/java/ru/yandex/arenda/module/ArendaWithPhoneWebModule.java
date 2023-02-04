package ru.yandex.arenda.module;

import ru.auto.tests.passport.module.PassportAccountWithPhoneModule;

public class ArendaWithPhoneWebModule extends DefaultModule {

    @Override
    protected void configure() {
        super.configure();
        install(new PassportAccountWithPhoneModule());
    }
}
