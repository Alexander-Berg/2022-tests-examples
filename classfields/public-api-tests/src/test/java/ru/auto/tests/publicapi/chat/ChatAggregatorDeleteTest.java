package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMercedesIrkAccount;
@Ignore
@DisplayName("DELETE /chat/aggregator")
@GuiceModules(PublicApiDealerModule.class)
@RunWith(GuiceTestRunner.class)
public class ChatAggregatorDeleteTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldDeleteChatAggregator() {
        String sessionId = adaptor.login(getMercedesIrkAccount()).getSession().getId();
        String channelName = getRandomString();
        adaptor.createChatAggregator(sessionId, channelName);

        api.chat().deleteAggregator()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));
    }

}
