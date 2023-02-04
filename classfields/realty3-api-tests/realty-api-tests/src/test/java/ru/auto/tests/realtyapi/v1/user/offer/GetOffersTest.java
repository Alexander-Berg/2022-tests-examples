package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.UserOffersResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("GET /user/offers")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetOffersTest {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    @Issue("REALTY-13817")
    public void shouldSee403WithoutHeaders() {
        api.userOffers().getUserOffersRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.userOffers().getUserOffersRoute().reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Issue("REALTY-13890")
    public void shouldGetEmpty() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        UserOffersResp resp = api.userOffers().getUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBeOK())).as(UserOffersResp.class, GSON);
        assertThat(resp.getResponse()).hasNoOffers();
        assertThat(resp.getResponse().getPager()).hasPage(1L).hasPageSize(DEFAULT_PAGE_SIZE)
                .hasTotalPages(1L).hasTotalItems(0L);
    }

    @Test
    public void shouldGetOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String id = adaptor.createOffer(token).getResponse().getId();

        UserOffersResp resp = api.userOffers().getUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBeOK())).as(UserOffersResp.class, GSON);
        assertThat(resp.getResponse().getOffers()).hasSize(1);
        assertThat(resp.getResponse().getOffers().get(0)).hasId(id);
        assertThat(resp.getResponse().getPager()).hasPage(1L).hasPageSize(DEFAULT_PAGE_SIZE)
                .hasTotalPages(1L).hasTotalItems(1L);
    }

    @Test
    public void shouldEmptyHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        JsonObject resp = api.userOffers().getUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBeOK())).as(JsonObject.class, GSON);

        JsonObject respProd = prodApi.userOffers().getUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBeOK())).as(JsonObject.class, GSON);
        MatcherAssert.assertThat(resp, jsonEquals(respProd));
    }
}
