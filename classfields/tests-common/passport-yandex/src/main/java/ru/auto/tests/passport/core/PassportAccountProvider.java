package ru.auto.tests.passport.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.config.PassportConfig;

import static ru.auto.tests.passport.client.Constants.COMMON_PASSWD1234567;
import static ru.auto.tests.passport.client.Constants.DEFAULT_FIRSTNAME;
import static ru.auto.tests.passport.core.PassportUtils.getRandomYandexTeamLogin;

/**
 * Created by vicdev on 10.04.17.
 */
public class PassportAccountProvider extends AbstractModule {

    @Provides
    public Account providesPassportYandexAccount(PassportConfig config, AccountKeeper accountKeeper, PassportAdaptor adaptor) {
        try {
            PassportUtils.enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
            PassportUtils.setBaseScheme(config.env());
            PassportUtils.setConsumer(config.consumer());
            String login = getRandomYandexTeamLogin();
            String uid = adaptor.createPassportAccount(login);
            Account account = Account.builder().id(uid).login(login).password(COMMON_PASSWD1234567)
                    .name(DEFAULT_FIRSTNAME).build();
            accountKeeper.add(account);
            return account;
        } finally {
            PassportUtils.disableProxyIf(config.isLocalDebug());
        }
    }

    @Override
    protected void configure() {
    }
}