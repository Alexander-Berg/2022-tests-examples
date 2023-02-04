package ru.yandex.realty.rules;

import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.test.api.realty.service.user.user.subscriptions.responses.SubscriptionsResp;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.realty.adaptor.BackRtAdaptor;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by vicdev on 26.10.17.
 * VERTISTEST-563
 */
@Log4j
public class DeleteSubscriptionsRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private BackRtAdaptor backRt;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> deleteSubscriptions(a.getId()));

    }

    @Step("Удаляем все подписки у пользователя {uid}")
    private void deleteSubscriptions(String uid) {
        try {
            String prefix = "uid";
            List<SubscriptionsResp> respList = backRt.getSubscriptions(prefix, uid);
            respList.stream().map(SubscriptionsResp::getId).collect(toList())
                    .forEach(id -> backRt.deleteSubscriptions(id, prefix, uid));
        } catch (Throwable e) {
            log.info(String.format("Can't delete subscriptions with uid %s. Exception: %s", uid, e.getMessage()));
        }
    }
}
