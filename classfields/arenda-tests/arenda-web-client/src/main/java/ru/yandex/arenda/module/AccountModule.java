package ru.yandex.arenda.module;

import com.google.inject.AbstractModule;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.account.AdminAccountProvider;
import ru.yandex.arenda.annotations.AdminAccount;

public class AccountModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Account.class).annotatedWith(AdminAccount.class).toProvider(AdminAccountProvider.class);
    }
}
