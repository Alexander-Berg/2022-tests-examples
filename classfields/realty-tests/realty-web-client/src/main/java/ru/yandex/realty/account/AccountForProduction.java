package ru.yandex.realty.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AccountForProduction implements Provider<Account> {

    @Inject
    private AccountKeeper accountKeeper;

    private static final String LOGIN = "yndx-245448608";
    private static final String PASSW = "simple123456";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .build();
        accountKeeper.add(account);
        return account;
    }
}
