package ru.yandex.realty.account;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AccountWithRedirectPhone implements Provider<Account> {

    @Inject
    private AccountKeeper accountKeeper;

    //Чтобы была возможность создавать подменные номера под этим пользователем, нужно обратиться к @rmuzhikov
    private static final String LOGIN = "realty-autotests1";
    private static final String PASSW = "testqa123";
    private static final String UID = "4010509222";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        accountKeeper.add(account);
        return account;
    }
}
