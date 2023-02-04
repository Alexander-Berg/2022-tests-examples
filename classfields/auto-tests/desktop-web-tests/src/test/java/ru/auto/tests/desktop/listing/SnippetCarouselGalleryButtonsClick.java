package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ACTION;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.SHOW_VIN_REPORT;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchOffersUsedQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffersCarsExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;

@DisplayName("Листинг - переход по кнопкам в галерее сниппета")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetCarouselGalleryButtonsClick {

    private static final String SALE_ID = getRandomOfferId();

    private static final String HONDA = "honda";
    private static final String SHUTTLE = "shuttle";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                getSearchOffersUsedQuery())
                        .withResponseBody(
                                searchOffersCarsExample().setId(SALE_ID).getBody()),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_EXAMPLE).setId(SALE_ID).getResponse())
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Testing.class})
    @DisplayName("Жмем на кнопку продавца в галерее, тип листинга «Карусель»")
    public void shouldClickSellerInGalleryCarousel() {
        basePageSteps.onListingPage().getCarouselSale(0).gallerySellerButton().hover().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(HONDA).path(SHUTTLE).path(SALE_ID).path(SLASH)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Testing.class})
    @DisplayName("Жмем «Смотреть отчёт» в галерее, тип листинга «Карусель»")
    public void shouldClickVinReportInGalleryCarousel() {
        basePageSteps.onListingPage().getCarouselSale(0).galleryVinReport().hover().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(HONDA).path(SHUTTLE).path(SALE_ID).path(SLASH)
                .addParam(ACTION, SHOW_VIN_REPORT).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Testing.class})
    @DisplayName("Жмем на «Ещё фото» в галерее, тип листинга «Карусель»")
    public void shouldClickMorePhotoInGalleryCarousel() {
        basePageSteps.onListingPage().getCarouselSale(0).galleryMorePhotos().hover().click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(HONDA).path(SHUTTLE).path(SALE_ID).path(SLASH)
                .shouldNotSeeDiff();
    }

}
