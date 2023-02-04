package ru.auto.tests.passport.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.config.PassportConfig;

import java.util.Optional;

import static ru.auto.tests.passport.client.Constants.COMMON_PASSWD1234567;
import static ru.auto.tests.passport.client.Constants.DEFAULT_FIRSTNAME;
import static ru.auto.tests.passport.core.PassportUtils.getRandomFakePhoneInE164;
import static ru.auto.tests.passport.core.PassportUtils.getRandomYndxCaptchaNever;

/**
 * Created by vicdev on 10.07.17.
 */
public class PassportAccountCaptchaNeverWithPhoneProvider extends AbstractModule {

    @Provides
    public Account providesPassportYandexWithPhoneAccount(PassportConfig config, AccountKeeper accountKeeper, PassportAdaptor adaptor) {
        try {
            PassportUtils.enableProxyIf(config.isLocalDebug(), config.proxyHost(), config.proxyPort());
            PassportUtils.setBaseScheme(config.env());
            PassportUtils.setConsumer(config.consumer());
            String phone = getRandomFakePhoneInE164();
            //https://wiki.yandex-team.ru/oauth/token/#podrobneeokapche
            String login = getRandomYndxCaptchaNever();
            String uid = adaptor.createPassportAccountWithPhone(phone, login);
            Account account = Account.builder().id(uid).login(login).password(COMMON_PASSWD1234567)
                    .name(DEFAULT_FIRSTNAME).phone(Optional.of(phone)).build();
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
