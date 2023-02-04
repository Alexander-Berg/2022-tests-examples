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
import ru.auto.tests.publicapi.model.AutoApiCreateRoomRequest;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiRoomResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
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
 * Created by vicdev on 18.09.17.
 */

@DisplayName("POST /chat/room")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateRoomTest {

    static final String[] IGNORED_PATHS = {"offer_id", "offer.id", "offer.mobile_url", "offer.state.upload_url", "offer.url", "offer.price_info.create_timestamp", "offer.price_history[*].create_timestamp", "offer.additional_info.creation_date", "offer.created", "offer.services[*].create_date", "room.subject.offer.value.tags"};

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
    public void shouldSee403ForCreateChatWhenNoAuth() {
        api.chat().createRoom().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()).execute(validatedWith(shouldBe401NoAuth()));
    }

    @Test
    public void shouldCreateEmptyRoom() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);

        AutoApiRoomResponse roomResponse = api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()).xSessionIdHeader(loginResult.getSession().getId()).executeAs(validatedWith(shouldBeSuccess()));

        AutoApiRoomResponse roomResponseProd = prodApi.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()).xSessionIdHeader(loginResult.getSession().getId()).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat(roomResponse, jsonEquals(roomResponseProd));
    }

    @Test
    public void shouldCreateRoomWithNotExistUser() {
        String user = Utils.getRandomString();
        AutoApiLoginResponse loginResult = adaptor.login(am.create());
        AutoApiErrorResponse response = api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()
                .users(newArrayList(user)))
                .xSessionIdHeader(loginResult.getSession().getId()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);

        assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
                .hasDetailedError(String.format("Unsupported user %s for chats", user));
    }

    @Test
    public void shouldCreateRoomWithUser() {
        Account account = am.create();

        AutoApiLoginResponse loginResult = adaptor.login(account);

        JsonObject roomResponse = api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()
                .users(getUsers(account.getId())))
                .xSessionIdHeader(loginResult.getSession().getId()).execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        JsonObject roomResponseProd = prodApi.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()
                .users(getUsers(account.getId())))
                .xSessionIdHeader(loginResult.getSession().getId()).execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(roomResponse, jsonEquals(roomResponseProd));
    }

    public List<String> getUsers(String... uids) {
        List<String> result = newArrayList();
        Arrays.stream(uids).forEach(u -> result.add(String.format("user:%s", u)));
        return result;
    }
}
