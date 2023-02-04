package ru.yandex.arenda.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AdminAccountProvider implements Provider<Account> {

    @Inject
    private AccountKeeper accountKeeper;

    private static final String LOGIN = "ztest";
    private static final String PASSW = "Qwerty123";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .build();
        accountKeeper.add(account);
        return account;
    }
}
