package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Java6Assertions;
import org.hamcrest.MatcherAssert;
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
import ru.auto.tests.publicapi.model.AutoApiChatAggregatorResponse;
import ru.auto.tests.publicapi.model.AutoApiCreateChatAggregatorRequest;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /chat/aggregator/{aggregator}")
@GuiceModules(PublicApiDealerModule.class)
@RunWith(GuiceTestRunner.class)
public class ChatAggregatorCreateTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Prod
    @Inject
    private ApiClient prodApi;

    @Inject
    private Account account;

    private static final String CHANNEL = getRandomString();

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldCheckCreateChatAggregatorHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        final String username =  getRandomString();
        final String userEmail = getRandomEmail();
        final String password = getRandomString();
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat()
          .createAggregator()
          .aggregatorPath("bachata")
          .body(new AutoApiCreateChatAggregatorRequest()
            .channelName(CHANNEL)
            .userDisplayName(username)
            .userEmail(userEmail)
            .userPassword(password)
          )
          .reqSpec(defaultSpec())
          .xSessionIdHeader(sessionId)
          .execute(validatedWith(shouldBe200OkJSON()))
          .as(JsonObject.class);

        JsonObject response = req.apply(api);
        AutoApiChatAggregatorResponse resp = new JSON().deserialize(response.toString(), AutoApiChatAggregatorResponse.class);
        Java6Assertions.assertThat(resp.getChannelName()).isEqualTo(CHANNEL);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
