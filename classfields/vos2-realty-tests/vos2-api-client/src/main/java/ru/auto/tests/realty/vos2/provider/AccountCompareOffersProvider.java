package ru.auto.tests.realty.vos2.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;

public class AccountCompareOffersProvider implements Provider<Account> {

    @Inject
    private AccountKeeper accountKeeper;

    //В аккаунте 20 заготовленных объялений
    private static final String LOGIN = "offercompare";
    private static final String PASSW = "testqa";
    private static final String UID = "4011149320";

    @Override
    public Account get() {
        Account account = Account.builder()
                .login(LOGIN).password(PASSW)
                .id(UID).build();
        accountKeeper.add(account);
        return account;
    }
}
