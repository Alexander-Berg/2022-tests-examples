package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferTypeEnum.SELL;



@Title("GET /offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferWithSiteSearchInvalidParamTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter(0)
    public String invalidPage;

    @Parameterized.Parameters(name = "invalidPage={0}")
    public static Object[] getParameters() {
        return new String[]{
                "-1",
                getRandomString()
        };
    }


    @Test
    public void shouldSee400ForPageWithInvalidParam() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageQuery(invalidPage)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400ForPageSizeWithInvalidParam() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageQuery(invalidPage)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
