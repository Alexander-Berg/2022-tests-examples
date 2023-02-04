package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Java6Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.DealerAccounts;

import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getBMWEurosibAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMaseratiUralAccount;

@DisplayName("POST /chat/aggregator")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class ChatAggregatorAddDealerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    private Account account;

    @Prod
    @Inject
    private ApiClient prodApi;

    private final Account secondAccount = getBMWEurosibAccount();

    private static final String CHANNEL = "test_channel";

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldCheckAggregatorAddDealerHasNoDiffWithProduction() {
        //get aggregator from prepared account
        String sessionId = adaptor.login(getMaseratiUralAccount()).getSession().getId();
        AutoApiChatAggregatorResponse responseWithExistingAggregator = adaptor.getChatAggregator(sessionId);

        String sessionId2 = adaptor.login(secondAccount).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat()
          .addAggregator()
          .body(new AutoApiAddChatAggregatorRequest()
            .channelName(CHANNEL)
            .token(responseWithExistingAggregator.getToken())
            .hook(responseWithExistingAggregator.getHook())
          )
          .xSessionIdHeader(sessionId2)
          .reqSpec(defaultSpec())
          .execute(validatedWith(shouldBe200OkJSON()))
          .as(JsonObject.class);

        JsonObject response = req.apply(api);
        AutoApiChatAggregatorResponse resp2 = new JSON().deserialize(response.toString(), AutoApiChatAggregatorResponse.class);
        Java6Assertions.assertThat(resp2.getChannelName()).isEqualTo(CHANNEL);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @After
    public void deleteAggregatorForSecondAccount() {
        String sessionId = adaptor.login(secondAccount).getSession().getId();
        api.chat().deleteAggregator().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }
}
