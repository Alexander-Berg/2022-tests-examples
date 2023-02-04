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
import static ru.yandex.general.consts.CardStatus.CardDeactivateStatuses.SOLD_ON_YANDEX;
import static ru.yandex.general.consts.CardStatus.INACTIVE;
import static ru.yandex.general.consts.CardStatus.REMOVED;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.MAX_MEDIA_CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(OFFER_CARD_FEATURE)
@Feature(SCREENSHOT_TESTS)
@DisplayName("Скриншот карточки объявления, для покупателя, на разных разрешениях")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardBuyerScreenshotTest {

    private static final String ID = "12345";
    private static final String PHONE_CALL = "PhoneCall";

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
                {"Удаленный оффер для покупателя", mockCard(BASIC_CARD).setStatus(REMOVED)},
                {"Неактивный оффер для покупателя. причина «Продал на Яндексе»", mockCard(BASIC_CARD)
                        .setStatus(INACTIVE).setInactiveReason(SOLD_ON_YANDEX.getMockValue())},
                {"Карточка с максимум медиа для покупателя. связь - любая", mockCard(MAX_MEDIA_CARD).setPreferContactWay("Any")}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard.setIsOwner(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот карточки объявления, для покупателя, разрешение «375x812»")
    public void shouldSeeOfferCardForBuyerScreenshot() {
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
    @DisplayName("Скриншот карточки объявления, для покупателя, разрешение «375x3000»")
    public void shouldSeeOfferCardForBuyerFullScreenshot() {
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
    @DisplayName("Скриншот карточки объявления, для покупателя, разрешение «375x812»")
    public void shouldSeeOfferCardForBuyerDarkThemeScreenshot() {
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
    @DisplayName("Скриншот карточки объявления, для покупателя, разрешение «375x3000»")
    public void shouldSeeOfferCardForBuyerDarkThemeFullScreenshot() {
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
