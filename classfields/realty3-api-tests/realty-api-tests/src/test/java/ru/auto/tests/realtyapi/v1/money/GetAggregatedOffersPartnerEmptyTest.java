package ru.auto.tests.realtyapi.v1.money;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.restassured.builder.RequestSpecBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.testdata.PartnerUser;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.function.Consumer;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.getParametersForPartnerEmpty;

@Title("GET /money/spent/aggregated/offers/partner/{partnerId}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetAggregatedOffersPartnerEmptyTest {

    private static final String LEVEL_DAY = "day";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OAuth oAuth;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter(0)
    public Consumer<RequestSpecBuilder> reqSpec;

    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return getParametersForPartnerEmpty();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyList() {
        Account account = PartnerUser.PASSPORT_ACCOUNT;
        String token = oAuth.getToken(account);
        JsonArray response = api.money().partnerAggregatedWithOffersSpent()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .reqSpec(reqSpec)
                .levelQuery(LEVEL_DAY)
                .partnerIdPath(PartnerUser.PARTNER_ID)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonArray("response");

        Assertions.assertThat(response).isEmpty();
    }
}
