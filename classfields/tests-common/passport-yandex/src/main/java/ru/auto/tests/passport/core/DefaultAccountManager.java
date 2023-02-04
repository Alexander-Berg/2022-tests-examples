package ru.auto.tests.passport.core;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.config.PassportConfig;
import ru.auto.tests.passport.manager.AccountManager;

/**
 * Created by vicdev on 02.11.17.
 */
public class DefaultAccountManager implements AccountManager {

    private static final Logger LOG = Logger.getLogger(AccountManager.class);

    private final Provider<Account> accountProvider;

    @Inject
    private DefaultAccountManager(Provider<Account> accountProvider) {
        this.accountProvider = accountProvider;
    }

    @Inject
    private PassportConfig config;

    @Inject
    private PassportAdaptor passportAdaptor;

    @Override
    public Account create() {
        Account account = accountProvider.get();
        LOG.info(String.format("=== User created at %s: UID = %s, login = %s, password = %s, phone = %s",
                config.env(), account.getId(), account.getLogin(), account.getPassword(),
                account.getPhone()));
        return account;
    }

    @Override
    public void delete(String id) {
        try {
            PassportUtils.enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
            PassportUtils.setBaseScheme(config.env());
            PassportUtils.setConsumer(config.consumer());
            passportAdaptor.deleteAccount(id);
        } finally {
            PassportUtils.disableProxyIf(config.isLocalDebug());
        }
    }
}
