package ru.auto.tests.realtyapi.v1.phone;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /phone/canCreateRedirect/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class CanCreateRedirectTest {

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

    @Test
    @Owner(SCROOGE)
    public void callsForUserCreateRedirectCompareTest() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.phone().canCreateRedirectRoute()
                .reqSpec(authSpec())
                .uidPath(account.getId()).offerIdQuery(offerId).phoneQuery(account.getPhone())
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));

    }
}