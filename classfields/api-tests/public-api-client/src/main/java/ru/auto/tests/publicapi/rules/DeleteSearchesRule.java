package ru.auto.tests.publicapi.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiSearchInstance;
import ru.auto.tests.publicapi.utils.DeviceUidKeeper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

public class DeleteSearchesRule extends ExternalResource {

    private static final Logger log = Logger.getLogger(DeleteAccountRule.class);

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private DeviceUidKeeper deviceUidKeeper;

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    public DeleteSearchesRule() {
    }

    @Override
    protected void after() {
        this.accountKeeper.get().forEach(this::deleteSearchesForAuthUser);
        this.deviceUidKeeper.get().forEach(this::deleteSearchesForAnonUser);
        deviceUidKeeper.clear();
    }

    @Step("Удаляем все подписки у пользователя {account.login}:{account.password} ({account.id})")
    private void deleteSearchesForAuthUser(Account account) {
        try {
            String sessionId = adaptor.login(account).getSession().getId();
                List<String> ids = api.userFavorites().getSavedSearches().reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(Function.identity()).getSavedSearches().stream().map(AutoApiSearchInstance::getId).collect(Collectors.toList());
                for (String id : ids) {
                    api.userFavorites().deleteSavedSearch().searchIdPath(id)
                            .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(Function.identity());
                }

        } catch (Throwable e) {
            log.error(String.format("Can't delete favorites for uid %s", account.getId()), e);
        }

    }

    @Step("Удаляем все подписки у анонимного пользователя {deviceUid})")
    private void deleteSearchesForAnonUser(String deviceUid) {
        try {
            List<String> ids = api.userFavorites().getSavedSearches().reqSpec(defaultSpec()).xDeviceUidHeader(deviceUid).executeAs(Function.identity()).getSavedSearches().stream().map(AutoApiSearchInstance::getId).collect(Collectors.toList());
            for (String id : ids) {
                api.userFavorites().deleteSavedSearch().searchIdPath(id)
                        .reqSpec(defaultSpec()).xDeviceUidHeader(deviceUid).execute(Function.identity());
            }
        } catch (Throwable e) {
            log.error(String.format("Can't delete favorites for deviceUid %s", deviceUid), e);
        }
    }
}
