package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.config.RealtyApiConfig;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.io.File;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.management.PublicProfile.FILE_OVER_10MB;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@Tag(JURICS)
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublicProfileTradeMarkTest {

    private static final String TRADE_MARK_FILENAME = "trade_mark.jpg";

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
    private BasePageSteps basePageSteps;

    @Inject
    private RealtyApiConfig config;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поле товарный знак присутствует у агентства")
    public void shouldSeeTradeMarkField() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().inputFile().should(exists());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поле товарный знак отсутствует у агента")
    public void shouldNotSeeTradeMarkField() {
        apiSteps.createVos2Account(account, AccountType.AGENT);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().should(not(exists()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сохраняем товарный знак")
    public void shouldAddTradeMark() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);

        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        addTradeMark(TRADE_MARK_FILENAME);
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        managementSteps.onManagementNewPage().settingsContent().message("Произошла ошибка")
                .waitUntil(not(isDisplayed()), 20);
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().inputFile()
                .should(exists());
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().buttonWithTitle(TRADE_MARK_FILENAME)
                .should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем товарный знак")
    public void shouldDeleteTradeMark() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);

        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        addTradeMark(TRADE_MARK_FILENAME);
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().inputFile()
                .should(exists());
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().buttonWithTitle(TRADE_MARK_FILENAME)
                .should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Загружаем товарный знак > 10 мб -> видим сообщение о недопустимости")
    public void shouldAddBigTradeMark() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);

        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        addTradeMark("trade_mark_10mb.jpg");
        managementSteps.onManagementNewPage().settingsContent().message(FILE_OVER_10MB).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот загруженного товарного знака")
    public void shouldSeeTradeMarkScreenshot() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);

        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        addTradeMark(TRADE_MARK_FILENAME);
        Screenshot testing = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock());

        urlSteps.setProductionHost().open();
        addTradeMark(TRADE_MARK_FILENAME);
        Screenshot production = compareSteps.takeScreenshot(
                managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Добавляем торговый знак")
    public void addTradeMark(String name) {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) basePageSteps.getDriver()).setFileDetector(new LocalFileDetector());
        }
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().inputFile().sendKeys(
                new File(format("src/test/resources/offer/%s", name)).getAbsolutePath());
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().inputFile()
                .waitUntil(not(exists()));
        managementSteps.onManagementNewPage().settingsContent().tradeMarkBlock().buttonWithTitle(name)
                .waitUntil(isDisplayed(), 30);
    }
}
