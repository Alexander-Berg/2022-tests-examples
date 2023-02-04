package ru.auto.tests.desktop.module;

import com.google.inject.AbstractModule;
import ru.auto.tests.commons.guice.CustomScopes;
import ru.auto.tests.desktop.TestData;
import ru.auto.tests.passport.account.Account;

public class TestDataModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Account.class).toProvider(TestData.OWNER_USER_PROVIDER).in(CustomScopes.THREAD);
    }

}
