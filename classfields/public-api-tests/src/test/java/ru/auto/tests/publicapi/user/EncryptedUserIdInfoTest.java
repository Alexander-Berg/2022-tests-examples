package ru.auto.tests.publicapi.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiUserInfoResponse;
import ru.auto.tests.publicapi.model.AutoApiUserResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /user/{encryptedUserID}/info")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EncryptedUserIdInfoTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee404WithIncorrectEncryptedUserId() {
        Account account = am.create();
        Account account2 = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String sessionId2 = adaptor.login(account2).getSession().getId();

        adaptor.setAllowOffersShow(sessionId2);

        String encryptedUser2Id = getRandomString();

        api.user()
                .getUserInfo()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .encryptedUserIDPath(encryptedUser2Id)
                .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldReturnExpectedUser() {
        Account account = am.create();
        Account account2 = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String sessionId2 = adaptor.login(account2).getSession().getId();

        adaptor.setAllowOffersShow(sessionId2);

        AutoApiUserResponse userResponse = adaptor.getUser(sessionId2, account2.getId());
        String encryptedUser2Id = userResponse.getEncryptedUserId();
        String expectedAlias = userResponse.getUser().getProfile().getAutoru().getAlias();

        AutoApiUserInfoResponse response = api.user()
                .getUserInfo()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .encryptedUserIDPath(encryptedUser2Id)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        Assertions.assertThat(response.getAlias()).isEqualTo(expectedAlias);
    }

    @Test
    public void shouldModerationStatusHasNoDiffWithProduction() {
        Account account = am.create();
        Account account2 = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String sessionId2 = adaptor.login(account2).getSession().getId();

        adaptor.setAllowOffersShow(sessionId2);

        AutoApiUserResponse userResponse = adaptor.getUser(sessionId2, account2.getId());
        String encryptedUser2Id = userResponse.getEncryptedUserId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.user()
                .getUserInfo()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .encryptedUserIDPath(encryptedUser2Id)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
