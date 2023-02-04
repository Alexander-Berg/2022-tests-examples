package ru.auto.tests.publicapi.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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

import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /chat/room/check-exists")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class CheckRoomExistsTest {

    private static final String OFFER_ID = "1097208758-9a8a6705";

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
    public void shouldSeeRoomExistByRoomId() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiRoomResponse room = adaptor.createRoom(sessionId, newArrayList(account));

        AutoApiRoomExistsResponse response = api.chat().checkRoomExists()
                .reqSpec(defaultSpec())
                .body(new AutoApiRoomLocator().roomId(room.getRoom().getId()))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasRoomExists(true);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeRoomExistByUsedId() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createRoom(sessionId, newArrayList(account));

        AutoApiRoomExistsResponse response = api.chat().checkRoomExists()
                .reqSpec(defaultSpec())
                .body(new AutoApiRoomLocator().source(new AutoApiCreateRoomRequest()
                        .addUsersItem(format("user:%s", account.getId()))))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasRoomExists(true);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeRoomExistBySubject() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createRoomWithOfferId(sessionId, newArrayList(account), CARS, OFFER_ID);

        AutoApiChatOfferSubjectSource offerSubjectSource = new AutoApiChatOfferSubjectSource()
                .category(CARS.getValue()).id(OFFER_ID);

        AutoApiRoomExistsResponse response = api.chat().checkRoomExists()
                .reqSpec(defaultSpec())
                .body(new AutoApiRoomLocator().source(new AutoApiCreateRoomRequest().subject(
                        new AutoApiChatSubjectSource().offer(offerSubjectSource))))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasRoomExists(true);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSeeRoomNotExist() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiRoomExistsResponse response = api.chat().checkRoomExists()
                .reqSpec(defaultSpec())
                .body(new AutoApiRoomLocator().roomId(getRandomString()))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response).hasRoomExists(false);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldCheckRoomExistsHasNoDiffWithProduction() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiRoomResponse room = adaptor.createRoom(sessionId, newArrayList(account));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.chat().checkRoomExists()
                .reqSpec(defaultSpec())
                .body(new AutoApiRoomLocator().roomId(room.getRoom().getId()))
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
