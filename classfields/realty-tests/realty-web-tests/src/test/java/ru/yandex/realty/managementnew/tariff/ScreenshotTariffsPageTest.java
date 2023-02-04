package ru.yandex.realty.managementnew.tariff;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.TARIFFS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.management.StickyTariff.CHOOSE_TARIFF;
import static ru.yandex.realty.element.management.StickyTariff.EXTENDED_BLOCK;
import static ru.yandex.realty.element.management.StickyTariff.MAXIMUM_BLOCK;
import static ru.yandex.realty.element.management.StickyTariff.MINIMUM_BLOCK;
import static ru.yandex.realty.element.management.StickyTariff.YOUR_CURRENT_TARIFF_BUTTON;
import static ru.yandex.realty.element.management.TariffPopup.CHOOSE_THIS_TARIFF;

@Tag(JURICS)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница тарифа")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ScreenshotTariffsPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот всей страницы")
    public void shouldSeeTariffsPageScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот плавающего блока")
    public void shouldSeeStickyBlockScreenshot() {
        compareSteps.resize(1640, 800);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.scrollDown(3000);
        managementSteps.onTariffsPage().stickyTariff().waitUntil(hasClass(containsString("__sticky")));
        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().stickyTariff());
        urlSteps.setProductionHost().open();
        managementSteps.scrollDown(3000);
        managementSteps.onTariffsPage().stickyTariff().waitUntil(hasClass(containsString("__sticky")));
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().stickyTariff());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа «Расширенный»")
    public void shouldSeeExtendedTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).click();

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().tariffPopup());

        urlSteps.setProductionHost().open();
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).click();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().tariffPopup());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа «Максимальный»")
    public void shouldSeeMaximumTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().tariffPopup());

        urlSteps.setProductionHost().open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().tariffPopup());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот после выбора тарифа «Расширенный»")
    public void shouldSeeAfterExtendedTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON).should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(CHOOSE_TARIFF)
                .should(isDisplayed());

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());

        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот после выбора тарифа «Максимальный»")
    public void shouldSeeAfterMaximumTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON)
                .should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(CHOOSE_TARIFF)
                .should(isDisplayed());

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());

        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот после перехода обратно на тариф «Минимальный»")
    public void shouldSeeAfterMinimumTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON)
                .should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(CHOOSE_TARIFF)
                .should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());

        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот после перехода обратно на тариф «Минимальный» а потом клик по продлить «Максимальный»")
    public void shouldSeeMaximumAfterMinimumTariffScreenshot() {
        compareSteps.resize(1640, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button("Продлить").click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON)
                .waitUntil(isDisplayed());
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));

        Screenshot testingScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());

        urlSteps.setProductionHost().open();
        Screenshot productionScreenshot = compareSteps
                .takeScreenshot(managementSteps.onTariffsPage().pageBody());
        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
