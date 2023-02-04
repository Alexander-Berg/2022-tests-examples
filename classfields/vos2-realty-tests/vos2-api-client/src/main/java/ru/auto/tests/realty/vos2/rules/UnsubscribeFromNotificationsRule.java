package ru.auto.tests.realty.vos2.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;


@Log4j
public class UnsubscribeFromNotificationsRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private Vos2ApiAdaptor adaptor;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> unsubscribeFromNotifications(a.getId()));
    }

    @Step("Отключаем оповещения пользователя {uid}")
    private void unsubscribeFromNotifications(String uid) {
        try {
            if (adaptor.isVosUser(uid)) {
                adaptor.unsubscribeUserFromNotifications(uid);
            }
        } catch (Throwable e) {
            log.info(String.format("Can't unsubscribe from notifications with uid %s. Exception: %s",
                    uid, e.getMessage()));
        }
    }
}
