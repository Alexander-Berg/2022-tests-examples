package ru.auto.tests.realtyapi.v1.common;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /deeplink.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeeplinkCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("url")
    @Parameterized.Parameter
    public String url;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> getParameters() {
        return newArrayList(
                "https://realty.yandex.ru/sankt-peterburg/kupit/kvartira/?metroTransport=",
                "https://realty.yandex.ru/moskva/kupit/novostrojka/solnechnyj-gorod/?rgid=193455&id=46511#site-proposals",
                "https://realty.yandex.ru/moskva/kupit/kvartira/?rgid=193455&priceMin=3000000&priceMax=4000000",
                "https://realty.yandex.ru/offer/4416734929894443521/",
                "https://realty.yandex.ru/sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/odnokomnatnaya/",
                "https://m.realty.yandex.ru/sankt-peterburg/kupit/kvartira/?metroTransport=",
                "https://m.realty.yandex.ru/moskva/kupit/novostrojka/solnechnyj-gorod/?rgid=193455&id=46511#site-proposals",
                "https://m.realty.yandex.ru/moskva/kupit/kvartira/?rgid=193455&priceMin=3000000&priceMax=4000000",
                "https://m.realty.yandex.ru/offer/4416734929894443521/",
                "https://m.realty.yandex.ru/sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/odnokomnatnaya/"
        );
    }

    @Test
    @Owner(SCROOGE)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.common().deeplinkRoute()
                .reqSpec(authSpec())
                .urlQuery(url)
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}


   