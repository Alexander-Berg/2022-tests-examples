package ru.auto.tests.realty.vos2.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;

public class Vos2AccountProvider implements Provider<Account> {

    @Inject
    private Vos2ApiAdaptor adaptor;

    @Inject
    private Account account;

    @Override
    public Account get() {
        adaptor.createUser(account.getId());
        return account;
    }
}
