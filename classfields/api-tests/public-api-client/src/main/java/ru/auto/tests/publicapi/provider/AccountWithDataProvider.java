package ru.auto.tests.publicapi.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

/**
 * Created by dskuznetsov on 26.07.18
 */

public class AccountWithDataProvider implements Provider<Account> {
    @Inject
    private AccountKeeper accountKeeper;

    public static final String LOGIN = "70010000000";
    public static final String PASSW = "autoru";
    public static final String UID = "35667561";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        accountKeeper.add(account);
        return account;
    }
}