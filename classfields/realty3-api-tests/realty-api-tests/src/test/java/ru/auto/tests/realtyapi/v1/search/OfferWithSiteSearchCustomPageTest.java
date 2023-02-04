package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.restassured.builder.ResponseSpecBuilder;
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

import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferTypeEnum.SELL;



@Title("GET /offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferWithSiteSearchCustomPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter(0)
    public int page;

    @Parameterized.Parameters(name = "page={0}")
    public static Object[] getParameters() {
        return new Integer[]{
                10,
                20,
                21
        };
    }

    @Test
    public void shouldSeeCustomPage() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL).categoryQuery(APARTMENT).pageQuery(page)
                .execute(validatedWith(shouldBePage(page)));
    }

    @Test
    @Issue("REALTY-14927")
    public void shouldSeeCustomPageSize() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageSizeQuery(page)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePageSize(page)));
    }

    private ResponseSpecBuilder shouldBePage(int page) {
        return shouldBe200Ok().expectBody("response.searchQuery.page", equalTo(page));
    }

    private ResponseSpecBuilder shouldBePageSize(int pageSize) {
        return shouldBe200Ok().expectBody("response.searchQuery.pageSize", equalTo(pageSize));
    }
}
