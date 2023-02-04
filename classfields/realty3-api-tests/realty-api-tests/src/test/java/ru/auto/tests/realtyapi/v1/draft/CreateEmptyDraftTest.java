package ru.auto.tests.realtyapi.v1.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.DraftSuccessResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("POST /user/offers/draft")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class CreateEmptyDraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.draft().createDraftRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.draft().createDraftRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.draft().createDraftRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())))
                .as(JsonObject.class, GSON);
    }

    @Test
    public void shouldCreateEmptyDraft() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        DraftSuccessResponse response = api.draft().createDraftRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeOK())).as(DraftSuccessResponse.class, GSON);

        Assertions.assertThat(response.getResponse().getDetails()).hasSize(1);
        assertThat(response.getResponse().getDetails().get(0))
                .hasId(response.getResponse().getId())
                .hasIsEligible(false);
    }
}
