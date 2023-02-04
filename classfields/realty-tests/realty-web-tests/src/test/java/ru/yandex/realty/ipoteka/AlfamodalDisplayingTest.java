package ru.yandex.realty.ipoteka;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.ALFABANK;
import static ru.yandex.realty.consts.Pages.IPOTEKA;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.element.AlfaBankMortgage.GET_DISCOUNT;
import static ru.yandex.realty.element.ipoteka.MortgageProgram.REGISTER_BUTTON;
import static ru.yandex.realty.page.AlfabankLandingPage.TAKE_DISCONT;
import static ru.yandex.realty.page.MortgageProgramCardPage.TAKE_APPLICATION;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * @author kantemirov
 */
@Link("https://st.yandex-team.ru/VERTISTEST-1746")
@DisplayName("Тесты на открытие формы заявки в ипотеке Альфабанка")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class AlfamodalDisplayingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        compareSteps.resize(1920, 5000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Листинг ипотек")
    public void shouldSeeAlfaPopupOnMortgageListing() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().hover();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().button(REGISTER_BUTTON).click();
        Screenshot testing = getScreenshot();

        urlSteps.setProductionHost().open();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().hover();
        basePageSteps.onIpotekaCalculatorPage().firstMortgageProgram().button(REGISTER_BUTTON).click();
        basePageSteps.onIpotekaCalculatorPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточка ипотечной программы")
    public void shouldSeeAlfaPopupOnMortgageCardPage() {
        urlSteps.testing().path(IPOTEKA).path("/alfa-bank-358023/")
                .path("/ipoteka-na-stroyashcheesya-zhilyo-2420173/").open();
        basePageSteps.onMortgageProgramCardPage().button(TAKE_APPLICATION).click();

        basePageSteps.onMortgageProgramCardPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.setProductionHost().open();
        basePageSteps.onMortgageProgramCardPage().button(TAKE_APPLICATION).click();

        basePageSteps.onMortgageProgramCardPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Лендос ипотеки альфа-банка")
    public void shouldSeeAlfaPopupOnAlfaLanding() {
        urlSteps.testing().path(ALFABANK).open();
        basePageSteps.onAlfabankLandingPage().button(TAKE_DISCONT).click();

        basePageSteps.onIpotekaCalculatorPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.setProductionHost().open();
        basePageSteps.onAlfabankLandingPage().button(TAKE_DISCONT).click();

        basePageSteps.onAlfabankLandingPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточка оффера")
    public void shouldSeeAlfaPopupOnOfferPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .queryParam(UrlSteps.NEW_FLAT_URL_PARAM, UrlSteps.NO_VALUE)
                .queryParam(UrlSteps.PRICE_MIN_URL_PARAM, "4500000")
                .queryParam(UrlSteps.PRICE_MAX_URL_PARAM, "12000000").open();

        basePageSteps.onOffersSearchPage().offer(FIRST).offerLink().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();

        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().alfaBankMortgageContainer());
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().alfaBankMortgageContainer());

        basePageSteps.onOfferCardPage().alfaBankMortgageContainer().button(GET_DISCOUNT).click();
        basePageSteps.onOfferCardPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.fromUri(urlSteps.getCurrentUrl()).setProductionHost().open();
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().alfaBankMortgageContainer());

        basePageSteps.onOfferCardPage().alfaBankMortgageContainer().button(GET_DISCOUNT).click();
        basePageSteps.onOfferCardPage().alfaPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private Screenshot getScreenshot() {
        return compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().alfaPopup());
    }

}
