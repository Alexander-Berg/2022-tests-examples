package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
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
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiVosOffersResponse;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/offers/byIds")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetUserOfferSnippetsByIdsCompareTest {

    private String uid;
    private String token;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Before
    public void createUser() {
        Account account = am.create();
        uid = getUid(account);
        token = oAuth.getToken(account);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeOneUserOffer() {
        String offerId = adaptor.createOffer(token).getResponse().getId();

        RealtyApiVosOffersResponse response = api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(uid)
                .offerIdQuery(offerId)
                .authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()));

        assertThat(response.getResponse().getOffers()).describedAs("Ответ должен содержать один оффер")
                .hasSize(1);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserSnippetHasNoDiffWithProduction() {
        String offerId = adaptor.createOffer(token).getResponse().getId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(uid)
                .offerIdQuery(offerId)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).when(IGNORING_ARRAY_ORDER));
    }
}
