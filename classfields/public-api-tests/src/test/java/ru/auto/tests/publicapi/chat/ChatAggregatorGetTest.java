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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.RAIV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMajorAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMaseratiUralAccount;
@Ignore
@DisplayName("GET /chat/aggregator")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class ChatAggregatorGetTest {

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

    private static final String CHANNEL = "test_channel";

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldGetNothingIfNoAggregator() {
        //login with dealer who has no aggregators
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();

        AutoApiErrorResponse resp2 = api.chat().getAggregator()
          .xSessionIdHeader(sessionId)
          .reqSpec(defaultSpec())
          .execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        assertThat(resp2).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR);
        assertThat(resp2).hasError(AutoApiErrorResponse.ErrorEnum.NOT_FOUND);
    }

    @Test
    @Ignore("Bachata недовольна кучей мусорных кабинетов")
    @Owner(RAIV)
    public void shouldCheckAggregatorGetDealerHasNoDiffWithProduction() {
        //login with dealer who has created aggregator
        String sessionId = adaptor.login(getMaseratiUralAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat()
          .getAggregator()
          .xSessionIdHeader(sessionId)
          .reqSpec(defaultSpec())
          .execute(validatedWith(shouldBe200OkJSON()))
          .as(JsonObject.class);

        JsonObject response = req.apply(api);
        AutoApiChatAggregatorResponse resp = new JSON().deserialize(response.toString(), AutoApiChatAggregatorResponse.class);
        Java6Assertions.assertThat(resp.getChannelName()).isEqualTo(CHANNEL);
        MatcherAssert.assertThat(response, jsonEquals(req.apply(prodApi)));
    }


}
