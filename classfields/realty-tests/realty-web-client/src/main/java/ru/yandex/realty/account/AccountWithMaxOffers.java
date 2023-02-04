package ru.yandex.realty.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AccountWithMaxOffers implements Provider<Account> {

    @Inject
    private AccountKeeper accountKeeper;

    //В аккаунте 21 заготовленное объяление
    private static final String LOGIN = "realty-autotests2";
    private static final String PASSW = "testqa123";
    private static final String UID = "4010512852";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        accountKeeper.add(account);
        return account;
    }
}
