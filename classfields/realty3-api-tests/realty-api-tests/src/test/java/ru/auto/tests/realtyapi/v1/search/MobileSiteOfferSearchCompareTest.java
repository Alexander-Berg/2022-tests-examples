package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Ignore;
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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getQueryMapParams;


@Title("GET /mobileSiteOfferSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MobileSiteOfferSearchCompareTest {

    private static final String RESOURCE_FILE = "search/mobile_site_offer_search_test_data.txt";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public Map<String, String> queryParams;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        return getQueryMapParams(RESOURCE_FILE);
    }

    @Test
    @Ignore
    public void shouldHasNoDiffWithProduction() {
//        JsonObject response = api.mobileSiteOfferSearchjson().reqSpec(req -> req.addQueryParams(queryParams))
//                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);
//
//        JsonObject responseProd = prodApi.mobileSiteOfferSearchjson().reqSpec(req -> req.addQueryParams(queryParams))
//                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);
//
//        assertThat(response, hasNoDiff(responseProd).ignore(".timeStamp"));
        //todo: ручка прокидывается напрямую
    }
}
