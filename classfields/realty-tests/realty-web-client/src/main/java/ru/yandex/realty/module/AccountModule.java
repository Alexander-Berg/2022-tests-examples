package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.account.AccountForProduction;
import ru.yandex.realty.account.AccountWithMaxOffers;
import ru.yandex.realty.account.AccountWithMaxOffersAgent;
import ru.yandex.realty.account.AccountWithRedirectPhone;
import ru.yandex.realty.anno.EnabledRedirectPhone;
import ru.yandex.realty.anno.MaxOffers;
import ru.yandex.realty.anno.MaxOffersAgent;
import ru.yandex.realty.anno.ProfsearchAccount;

public class AccountModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Account.class).annotatedWith(EnabledRedirectPhone.class).toProvider(AccountWithRedirectPhone.class);
        bind(Account.class).annotatedWith(MaxOffers.class).toProvider(AccountWithMaxOffers.class);
        bind(Account.class).annotatedWith(MaxOffersAgent.class).toProvider(AccountWithMaxOffersAgent.class);
        bind(Account.class).annotatedWith(ProfsearchAccount.class).toProvider(AccountForProduction.class);
    }
}
