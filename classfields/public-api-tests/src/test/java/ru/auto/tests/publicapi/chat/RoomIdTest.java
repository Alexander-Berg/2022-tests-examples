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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiRoomResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NoAuth;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("GET /chat/room/{id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class RoomIdTest {

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
    public void shouldSee403ForGetChatWhenNoAuth() {
        api.chat().getRoom().idPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String chatId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        api.chat().getRoom().idPath(chatId).reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldSee400WithNotExistId() {
        String chatId = Utils.getRandomString();
        AutoApiErrorResponse response = api.chat().getRoom().idPath(chatId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
                .hasDetailedError(String.format("Incorrect chat room id: [%s]", chatId));
    }

    @Test
    public void shouldSeeEmptyRoomAfterCreate() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        AutoApiRoomResponse roomResponse = api.chat().getRoom().idPath(roomId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));
        assertThat(roomResponse.getRoom()).hasId(roomId);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String roomId = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.chat().getRoom().idPath(roomId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
