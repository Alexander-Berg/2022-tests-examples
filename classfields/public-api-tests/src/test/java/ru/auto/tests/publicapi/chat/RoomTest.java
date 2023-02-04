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
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_LOGIN;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_PASS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiChatRoom.RoomTypeEnum;
import static ru.auto.tests.publicapi.model.AutoApiChatRoom.RoomTypeEnum.TECH_SUPPORT;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 18.09.17.
 */

@DisplayName("GET /chat/room")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class RoomTest {
    private static final int DEFAULT_TTL = 360;

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
        api.chat().getRooms().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        AutoApiErrorResponse response = api.chat().getRooms().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED))).as(AutoApiErrorResponse.class);
        assertThat(response).hasError(NO_AUTH).hasStatus(ERROR)
                .hasDetailedError("Expected registered user. Provide valid session_id");
    }

    @Test
    public void shouldGetEmptyBody() {
        String sessionId = adaptor.login(am.create()).getSession().getId();
        api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSeeEmptyRoomAfterCreate() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.createEmptyRoom(sessionId).getRoom().getId();
        // пустые комнаты не возвращаем
        AutoApiRoomListingResponse roomListingResponse = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess())).as(AutoApiRoomListingResponse.class);
        MatcherAssert.assertThat("rooms[] должен содержать 1 комнату (чат с техподдержкой): пустую комнату не возвращаем", roomListingResponse.getRooms(), hasSize(1));
        adaptor.createMessage(sessionId, id);
        // непустые - возвращаем
        AutoApiRoomListingResponse roomListingResponse2 = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess())).as(AutoApiRoomListingResponse.class);
        MatcherAssert.assertThat("rooms[] должен содержать 2 комнаты (чат с техподдержкой+созданная комната)", roomListingResponse2.getRooms(), hasSize(2));
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createEmptyRoom(sessionId);
        JsonObject roomListingResponse = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        JsonObject roomListingResponseProd = prodApi.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(roomListingResponse, jsonEquals(roomListingResponseProd).whenIgnoringPaths("rooms[*].created", "rooms[*].updated"));
    }

    @Test
    public void shouldSeeSupportChatForDealer() {
        String sessionId = api.auth().login().body(new VertisPassportLoginParameters().login(DEALER_LOGIN)
                .password(DEALER_PASS).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        AutoApiChatRoom room = api.chat().getRooms().reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess())).getRooms().get(0);

        AutoApiChatRoomAssert.assertThat(room).hasRoomType(TECH_SUPPORT);
    }
}