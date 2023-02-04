package ru.auto.tests.passport.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.manager.AccountManager;


/**
 * Created by vicdev on 17.08.17.
 */
@Log4j
public class DeleteAccountRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private AccountManager manager;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> deleteAccount(a.getId()));
        accountKeeper.clear();
    }

    @Step("Удаляем аккаунт: {uid}")
    private void deleteAccount(String uid) {
        try {
            manager.delete(uid);
        } catch (Throwable e) {
            log.info(String.format("Can't delete account with uid %s. Exception: %s", uid, e.getMessage()));
        }
    }
}
