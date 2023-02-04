package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.WRONG_OFFER_CATEGORY;
import static ru.yandex.general.consts.CardStatus.REMOVED;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.MAX_MEDIA_CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(OFFER_CARD_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот карточки объявления, для продавца, на разных разрешениях")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardOwnerScreenshotTest {

    private static final String ID = "12345";
    private static final int DAYS_COUNT = 7;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public MockCard mockCard;

    @Parameterized.Parameters(name = "{index}. Скриншот «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Удаленный оффер для продавца", mockCard(BASIC_CARD).setStatus(REMOVED)},
                {"Забаненный оффер для продавца две причины бана", mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM, WRONG_OFFER_CATEGORY)},
                {"Неактивный оффер для продавца причина «Продал на Яндексе»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_ON_YANDEX.getMockValue())},
                {"Карточка с максимум медиа для продавца", mockCard(MAX_MEDIA_CARD)}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(true).setStatisticsGraph(DAYS_COUNT).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID);

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карточки объявления, для продавца, разрешение «375x812»")
    public void shouldSeeOfferCardForOwnerScreenshot() {
        compareSteps.resize(375, 812);
        urlSteps.open();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карточки объявления, для продавца, разрешение «375x3000»")
    public void shouldSeeOfferCardForOwnerFullScreenshot() {
        compareSteps.resize(375, 3000);
        urlSteps.open();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карточки объявления, для продавца, разрешение «375x812»")
    public void shouldSeeOfferCardForOwnerDarkThemeScreenshot() {
        compareSteps.resize(375, 812);
        basePageSteps.setDarkThemeCookie();
        urlSteps.open();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карточки объявления, для продавца, разрешение «375x3000»")
    public void shouldSeeOfferCardForOwnerDarkThemeFullScreenshot() {
        compareSteps.resize(375, 3000);
        basePageSteps.setDarkThemeCookie();
        urlSteps.open();
        Screenshot testing = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.getElementScreenshotIgnoreAreas(
                basePageSteps.onOfferCardPage().pageRoot(),
                basePageSteps.onOfferCardPage().adBanner(),
                basePageSteps.onOfferCardPage().breadcrumbs());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
