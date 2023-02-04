package ru.yandex.realty.ipoteka.calculator;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
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
import static ru.yandex.realty.mobile.page.MortgageTouchPage.REGISTER_BUTTON;
import static ru.yandex.realty.step.CommonSteps.FIRST;

/**
 * @author kantemirov
 */
@Link("https://st.yandex-team.ru/VERTISTEST-1746")
@DisplayName("Тесты на открытие формы заявки в ипотеке Альфабанка")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AlfamodalTouchDisplaying {

    String TAKE_APPLICATION = "Подать заявку";
    String TAKE_DISCONT = "Получить скидку";

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
        compareSteps.resize(400, 1200);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Листинг мобильных ипотек")
    public void shouldSeeAlfaPopupOnMortgageTouchListing() {
        urlSteps.testing().path(IPOTEKA_CALCULATOR).open();
        basePageSteps.onMortgagePage().firstMortgageProgram().button(REGISTER_BUTTON).click();
        Screenshot testing = getScreenshot();

        urlSteps.setMobileProductionHost().open();
        basePageSteps.onMortgagePage().firstMortgageProgram().button(REGISTER_BUTTON).click();
        basePageSteps.onMortgagePage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточка мобильной ипотечной программы")
    public void shouldSeeAlfaPopupOnMortgageCardPage() {
        urlSteps.testing().path(IPOTEKA).path("/alfa-bank-358023/")
                .path("/ipoteka-na-stroyashcheesya-zhilyo-2420173/").open();
        basePageSteps.onBasePage().button(TAKE_APPLICATION).click();

        basePageSteps.onBasePage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.setMobileProductionHost().open();
        basePageSteps.onBasePage().button(TAKE_APPLICATION).click();

        basePageSteps.onBasePage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Мобильный лендос ипотеки альфа-банка")
    public void shouldSeeAlfaPopupOnTouchAlfaLanding() {
        urlSteps.testing().path(ALFABANK).open();
        basePageSteps.onBasePage().button(TAKE_DISCONT).click();

        basePageSteps.onMortgagePage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.setMobileProductionHost().open();
        basePageSteps.onBasePage().button(TAKE_DISCONT).click();

        basePageSteps.onBasePage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточка оффера")
    public void shouldSeeAlfaPopupOnTouchOfferPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA)
                .queryParam(UrlSteps.NEW_FLAT_URL_PARAM, UrlSteps.NO_VALUE)
                .queryParam(UrlSteps.PRICE_MIN_URL_PARAM, "4500000")
                .queryParam(UrlSteps.PRICE_MAX_URL_PARAM, "12000000").open();

        basePageSteps.onMobileSaleAdsPage().offer(FIRST).link().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();

        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer());
        basePageSteps.scrollElementToCenter(basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer());

        basePageSteps.scrollElementToCenter(
                basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer().button(GET_DISCOUNT));
        basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer().button(GET_DISCOUNT).click();
        basePageSteps.onMobileSaleAdsPage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot testing = getScreenshot();

        urlSteps.fromUri(urlSteps.getCurrentUrl()).setMobileProductionHost().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer());
        basePageSteps.scrollElementToCenter(
                basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer().button(GET_DISCOUNT));

        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer());
        basePageSteps.scrollElementToCenter(
                basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer().button(GET_DISCOUNT));
        basePageSteps.onMobileSaleAdsPage().alfaBankMortgageContainer().button(GET_DISCOUNT).click();
        basePageSteps.onMobileSaleAdsPage().alfaTouchPopup().waitUntil(isDisplayed());
        Screenshot production = getScreenshot();

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private Screenshot getScreenshot() {
        return compareSteps.takeScreenshot(basePageSteps.onMobileSaleAdsPage().alfaTouchPopup());
    }

}
