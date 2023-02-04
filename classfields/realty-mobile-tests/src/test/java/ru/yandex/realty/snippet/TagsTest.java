package ru.yandex.realty.snippet;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.SNIPPET;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1352")
@DisplayName("Тэги на сниппете")
@Feature(SNIPPET)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class TagsTest {

    private static final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final int NEW_BUILDING_SITE_ID = 12441;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «онлайн показ»")
    public void shouldSeeOnlineShowTag() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(SELL_APARTMENT).addRemoteView())).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).galleryTag("онлайн показ").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «есть видео»")
    public void shouldSeeGotVideoTag() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(SELL_APARTMENT).addVideoLink(YOUTUBE_LINK))).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).galleryTag("есть видео").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «хорошая цена»")
    public void shouldSeeGoodPriceTag() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(SELL_APARTMENT).addPredictedPriceAdviceLow())).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).priceTag("хорошая цена").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «проверено в Росреестре»")
    public void shouldSeeCheckedInEGRNTag() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(SELL_APARTMENT).addFreeReportAccessibility())).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).galleryTag("проверено в Росреестре").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «новостройка»")
    public void shouldSeeNewBuildingLabel() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(SELL_APARTMENT).setBuildingSiteId(NEW_BUILDING_SITE_ID))).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).galleryTag("новостройка").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тэг «от Яндекс.Аренды»")
    public void shouldSeeYandexRentLabel() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(
                mockOffer(RENT_APARTMENT).addYandexRen())).build()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(KVARTIRA).open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).galleryArendaTag().should(isDisplayed());
    }

}
