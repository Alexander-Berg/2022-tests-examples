package ru.auto.tests.passport.manager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.extern.log4j.Log4j;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;

/**
 * Created by vicdev on 02.11.17.
 */
@Log4j
public class DefaultAccountManager implements AccountManager {

    private final Provider<Account> accountProvider;

    @Inject
    private DefaultAccountManager(Provider<Account> accountProvider) {
        this.accountProvider = accountProvider;
    }

    @Inject
    private PassportApiAdaptor adaptor;

    @Override
    public void delete(String id) {
        adaptor.deleteAccount(id);
    }

    @Override
    public Account create() {
       return accountProvider.get();
    }
}
