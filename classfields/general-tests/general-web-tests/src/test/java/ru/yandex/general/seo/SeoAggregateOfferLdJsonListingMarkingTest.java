package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.page.BasePage;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static ru.yandex.general.beans.ldJson.AggregateOffer.aggregateOffer;
import static ru.yandex.general.consts.GeneralFeatures.AGGREGATE_OFFER_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;

@Epic(SEO_FEATURE)
@Feature(AGGREGATE_OFFER_SEO_MARK)
@DisplayName("LD-JSON разметка AggregateOffer на листинге")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoAggregateOfferLdJsonListingMarkingTest {

    private static final int PRICE_MIN = 0;
    private static final int PRICE_MAX = 152345;
    private static final int OFFER_COUNT = 13;

    private MockResponse mockResponse = mockResponse();

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Rule
    @Inject
    public MockRuleWithoutWebdriver mockRule;

    @Before
    public void before() {
        mockResponse.setCategoriesTemplate()
                .setRegionsTemplate();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка AggregateOffer на листинге с офферами")
    public void shouldSeeLdJsonAggregateOfferOnListingWithOffers() {
        mockRule.graphqlStub(mockResponse.setSearch(
                        listingCategoryResponse().addOffers(OFFER_COUNT).setPriceStatictics(PRICE_MIN, PRICE_MAX).build())
                .build()).withDefaults().create();
        jSoupSteps.testing().path(ELEKTRONIKA).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        String expectedAggregateOffer = aggregateOffer()
                .setOfferCount(OFFER_COUNT)
                .setLowPrice(PRICE_MIN)
                .setHighPrice(PRICE_MAX).toString();
        String actualAggregateOffer = jSoupSteps.getLdJsonMark(BasePage.AGGREGATE_OFFER);

        assertThatJson(actualAggregateOffer).isEqualTo(expectedAggregateOffer);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("LD-JSON разметка AggregateOffer на листинге без офферов")
    public void shouldSeeLdJsonAggregateOfferOnListingWithoutOffers() {
        mockRule.graphqlStub(mockResponse.setSearch(
                        listingCategoryResponse().addOffers(0).setNullPriceStatictics().build())
                .build()).withDefaults().create();
        jSoupSteps.testing().path(ELEKTRONIKA).setMockritsaImposter(mockRule.getPort())
                .setDesktopUserAgent().get();

        String expectedAggregateOffer = aggregateOffer()
                .setOfferCount(0)
                .setLowPrice(0)
                .setHighPrice(0).toString();
        String actualAggregateOffer = jSoupSteps.getLdJsonMark(BasePage.AGGREGATE_OFFER);

        assertThatJson(actualAggregateOffer).isEqualTo(expectedAggregateOffer);
    }

}
