package ru.auto.tests.passport.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;


@Log4j
public class UnlinkUserFromDealerRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private PassportApiAdaptor adaptor;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> unlinkUserFromDealer(a.getId()));
    }

    @Step("Отвязываем аккаунт {uid} от дилера")
    private void unlinkUserFromDealer(String uid) {
        try {
            adaptor.unlinkUserFromClient(uid);
        } catch (Throwable e) {
            log.info(String.format("Can't unlink account with uid %s from dealer. Exception: %s", uid, e.getMessage()));
        }
    }
}
