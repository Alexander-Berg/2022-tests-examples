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
import ru.auto.tests.publicapi.model.VertisSharkCreditApplication;
import ru.auto.tests.publicapi.model.VertisSharkCreditApplicationSource;
import ru.auto.tests.publicapi.utils.CollectionUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

public class CancelSharkCreditApplicationsRule extends ExternalResource {

    private static final Logger log = Logger.getLogger(CancelSharkCreditApplicationsRule.class);

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Override
    protected void after() {
        this.accountKeeper.get().forEach(this::cancelUserCreditApplications);
    }

    @Step("Отменяем кредитные заявки пользователя {account.login}:{account.password} ({account.id})")
    private void cancelUserCreditApplications(Account account) {
        try {
            String sessionId = adaptor.login(account).getSession().getId();

            List<String> ids = CollectionUtils.emptyIfNull(
                    api.shark().creditApplicationList()
                        .reqSpec(defaultSpec())
                        .xSessionIdHeader(sessionId)
                        .executeAs(Function.identity())
                        .getCreditApplications()
                )
                .stream()
                .map(VertisSharkCreditApplication::getId)
                .collect(Collectors.toList());
            for (String id : ids) {
                api.shark()
                    .creditApplicationUpdate()
                    .reqSpec(defaultSpec())
                    .xSessionIdHeader(sessionId)
                    .creditApplicationIdPath(id)
                    .body(
                        new VertisSharkCreditApplicationSource()
                            .state(VertisSharkCreditApplicationSource.StateEnum.CANCELED))
                    .executeAs(Function.identity());
            }

        } catch (Throwable e) {
            log.error(String.format("Can't cancel credit applications for uid %s", account.getId()), e);
        }
    }
}
