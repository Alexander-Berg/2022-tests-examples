package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.rules.MockRuleWithoutWebdriver;
import ru.yandex.general.step.JSoupSteps;

import static ru.yandex.general.consts.GeneralFeatures.AGGREGATE_OFFER_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;

@Epic(SEO_FEATURE)
@Feature(AGGREGATE_OFFER_SEO_MARK)
@DisplayName("ShemaOrg разметка AggregateOffer на листинге")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoAggregateOfferShemaOrgListingMarkingTest {

    private static final int PRICE_MIN = 0;
    private static final int PRICE_MAX = 152345;
    private static final int OFFERS_COUNT = 13;
    private static final String IMAGE_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/c1467974d9b3fedd86bab1f356cdb62f/260x346";

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
    @DisplayName("Schema.org разметка AggregateOffer на листинге с офферами")
    public void shouldSeeShemaOrgAggregateOfferOnListingWithOffers() {
        mockRule.graphqlStub(mockResponse.setSearch(
                        listingCategoryResponse().addOffers(OFFERS_COUNT).setPriceStatictics(PRICE_MIN, PRICE_MAX).build())
                .build()).withDefaults().create();
        jSoupSteps.testing().path(ELEKTRONIKA).setMockritsaImposter(mockRule.getPort())
                .setMobileUserAgent().get();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(jSoupSteps.getItempropContent("name")).as("Название категории").isEqualTo("Электроника");
            s.assertThat(jSoupSteps.select("div[itemprop='offers']").attr("itemtype")).as("itemtype").isEqualTo("http://schema.org/AggregateOffer");
            s.assertThat(jSoupSteps.select("link[itemprop='image']").attr("href")).as("Ссылка фото").isEqualTo(IMAGE_URL);
            s.assertThat(jSoupSteps.getItempropContent("lowPrice")).as("Минимальная цена").isEqualTo(String.valueOf(PRICE_MIN));
            s.assertThat(jSoupSteps.getItempropContent("highPrice")).as("Максимальная цена").isEqualTo(String.valueOf(PRICE_MAX));
            s.assertThat(jSoupSteps.getItempropContent("offerCount")).as("Кол-во офферов").isEqualTo(String.valueOf(OFFERS_COUNT));
            s.assertThat(jSoupSteps.getItempropContent("priceCurrency")).as("Тип валюты").isEqualTo("RUB");
        });
    }

}
