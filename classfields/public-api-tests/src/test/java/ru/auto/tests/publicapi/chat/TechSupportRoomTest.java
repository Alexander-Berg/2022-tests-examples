package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 16.08.18.
 */

@DisplayName("GET /chat/room/tech-support")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class TechSupportRoomTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.chat().getTechSupportRoom().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        api.chat().getTechSupportRoom().reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSeeNoDiffWithRoomList() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String roomIdFromListing = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess())).getRooms().get(0).getId();

        String roomIdTechSupport = api.chat().getTechSupportRoom().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess())).getRoom().getId();

        assertThat(roomIdFromListing).isEqualTo(roomIdTechSupport);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat().getTechSupportRoom().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths("room.created", "room.updated"));
    }
}
