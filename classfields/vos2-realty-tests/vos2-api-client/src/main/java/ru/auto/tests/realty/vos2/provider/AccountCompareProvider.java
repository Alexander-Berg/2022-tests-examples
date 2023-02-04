package ru.auto.tests.realty.vos2.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AccountCompareProvider implements Provider<Account> {
    @Inject
    private AccountKeeper accountKeeper;

    //В аккаунте 20 заготовленных объялений
    private static final String LOGIN = "ya15092016";
    private static final String PASSW = "sasha123";
    private static final String UID = "4003976033";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        accountKeeper.add(account);
        return account;
    }
}
