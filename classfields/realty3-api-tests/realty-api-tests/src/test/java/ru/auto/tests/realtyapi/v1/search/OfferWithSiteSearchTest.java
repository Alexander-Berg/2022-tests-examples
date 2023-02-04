package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.restassured.builder.ResponseSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.Payload.OfferTypeEnum.SELL;


@Title("GET /offerWithSiteSearch.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class OfferWithSiteSearchTest {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSeeDefaultPage() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePage(DEFAULT_PAGE)));
    }

    @Test
    public void shouldSeeDefaultPageSize() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePageSize(DEFAULT_PAGE_SIZE)));
    }

    @Test
    public void shouldSeeDefaultPageSizeForZero() {
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageSizeQuery(0)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePageSize(DEFAULT_PAGE_SIZE)));
    }

    @Test
    public void shouldSeePageIfPageGreaterDefaultPageSize() {
        int page = DEFAULT_PAGE_SIZE + getRandomShortInt();
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageQuery(page)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePage(page)));
    }

    @Test
    public void shouldSeeCustomPageIfPageGreaterPageSize() {
        int pageSize = getRandomShortInt();
        int page = pageSize + getRandomShortInt();
        api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .pageQuery(page)
                .typeQuery(SELL).categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBePage(page)));
    }

    private ResponseSpecBuilder shouldBePage(int page) {
        return shouldBe200Ok().expectBody("response.searchQuery.page", equalTo(page));
    }

    private ResponseSpecBuilder shouldBePageSize(int pageSize) {
        return shouldBe200Ok().expectBody("response.searchQuery.pageSize", equalTo(pageSize));
    }
}
