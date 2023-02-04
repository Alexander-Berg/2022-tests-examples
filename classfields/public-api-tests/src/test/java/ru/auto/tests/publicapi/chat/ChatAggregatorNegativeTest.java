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
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAddChatAggregatorRequest;
import ru.auto.tests.publicapi.model.AutoApiChatAggregatorResponse;
import ru.auto.tests.publicapi.model.AutoApiCreateChatAggregatorRequest;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMaseratiUralAccount;
@Ignore
@DisplayName("POST /chat/aggregator, POST /chat/aggregator/{aggregator}")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class ChatAggregatorNegativeTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    private static final String CHANNEL = "test_channel";

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldNotAddChatAggregatorIfNotADealer() {
        String sessionId = adaptor.login(getMaseratiUralAccount()).getSession().getId();
        //get aggregator from prepared account
        AutoApiChatAggregatorResponse resp = adaptor.getChatAggregator(sessionId);

        Account notDealerAccount = am.create();
        String sessionId2 = adaptor.login(notDealerAccount).getSession().getId();

        AutoApiErrorResponse resp2 = api.chat().addAggregator()
                .body(new AutoApiAddChatAggregatorRequest()
                        .channelName(CHANNEL)
                        .token(resp.getToken())
                        .hook(resp.getHook())
                )
                .xSessionIdHeader(sessionId2)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);

        assertThat(resp2).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR);
        assertThat(resp2).hasError(AutoApiErrorResponse.ErrorEnum.NO_AUTH);
        assertThat(resp2).hasDetailedError("Expected dealer user. Provide valid session_id");

    }

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldNotCreateChatAggregatorIfNotADealer() {
        Account notDealerAccount = am.create();
        String sessionId = adaptor.login(notDealerAccount).getSession().getId();

        AutoApiErrorResponse resp = api.chat().createAggregator().aggregatorPath("bachata")
                .body(new AutoApiCreateChatAggregatorRequest()
                        .channelName(CHANNEL)
                        .userDisplayName(getRandomString())
                        .userEmail(getRandomEmail())
                        .userPassword(getRandomString())
                )
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(resp).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR);
        assertThat(resp).hasError(AutoApiErrorResponse.ErrorEnum.NO_AUTH);
        assertThat(resp).hasDetailedError("Expected dealer user. Provide valid session_id");

    }
}
