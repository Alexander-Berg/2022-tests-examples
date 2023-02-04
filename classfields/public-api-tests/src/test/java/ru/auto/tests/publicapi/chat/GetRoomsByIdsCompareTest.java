package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiRoomResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /chat/room/by-id")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetRoomsByIdsCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(TIMONDL)
    public void shouldGetRoomsByIdsHasNoDiffWithProduction() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiRoomResponse room = adaptor.createRoom(sessionId, newArrayList(account));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat().getRoomsByIds()
                .reqSpec(defaultSpec())
                .idQuery(room.getRoom().getId())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
