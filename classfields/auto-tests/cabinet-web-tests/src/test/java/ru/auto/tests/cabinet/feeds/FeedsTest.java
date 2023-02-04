package ru.auto.tests.cabinet.feeds;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_PRICE_LIST;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.AUTO;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.DELETE_FEED;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.DELETE_MANUAL_CHECKBOX;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.DONT_DELETE_PHOTO_CHECKBOX;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.DONT_DELETE_SERVICES_CHECKBOX;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.HEAVY_TRUCKS;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.MANUAL;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.NOTIFY_SUCCESS;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.TRUCKS;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.CARS_USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Действия на странице фида")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FeedsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/FeedsSettingsEmpty").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по категории «Легковые с пробегом» + Выбор типа загрузки «Ручная» + чекбоксы")
    public void shouldClickCarCategory() {
        chooseCategory(CARS_USED, MANUAL);
        fillCheckbox();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());

        urlSteps.setProduction().open();
        chooseCategory(CARS_USED, MANUAL);
        fillCheckbox();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по категории «Тяжелые коммерческие с пробегом» + Выбор типа загрузки «Автоматическая»")
    public void shouldClickLVCategory() {
        chooseCategory(HEAVY_TRUCKS, AUTO);
        steps.onCabinetFeedsPage().category(TRUCKS).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());
        urlSteps.setProduction().open();
        chooseCategory(HEAVY_TRUCKS, AUTO);
        steps.onCabinetFeedsPage().category(TRUCKS).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());
        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Нажать на кнопку «Удалить фид»")
    public void shouldClickDeleteFeed() {
        chooseCategory(CARS_USED, MANUAL);
        steps.onCabinetFeedsPage().button(DELETE_FEED).waitUntil(isDisplayed()).hover().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());

        urlSteps.setProduction().open();
        chooseCategory(CARS_USED, MANUAL);
        steps.onCabinetFeedsPage().button(DELETE_FEED).waitUntil(isDisplayed()).hover().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().addFeedBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Добавить фид при выборе автоматического типа загрузки")
    public void shouldAddFeed() {
        mockRule.with("cabinet/FeedsSettingsCarsUsed").update();

        chooseCategory(CARS_USED, AUTO);
        steps.onCabinetFeedsPage().input("Укажите URL фида").sendKeys("http://example.com/feed/");
        steps.onCabinetFeedsPage().button("Добавить фид").click();
        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed()).should(hasText(NOTIFY_SUCCESS));
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по ссылке «Как подготовить фид»")
    public void shouldOpenHelpLink() {
        steps.onCabinetFeedsPage().button("Как подготовить фид").click();
        steps.switchToNextTab();
        urlSteps.fromUri(YANDEX_SUPPORT_AUTORU_PRICE_LIST).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на вкладку «История загрузок»")
    public void shouldOpenHistory() {
        mockRule.with("cabinet/FeedsHistory").update();

        steps.onCabinetFeedsPage().linkNavigation("История загрузок").click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).shouldNotSeeDiff();
    }

    @Step("Кликаем чекбоксы")
    public void fillCheckbox() {
        steps.onCabinetFeedsPage().checkbox(DELETE_MANUAL_CHECKBOX).click();
        steps.onCabinetFeedsPage().checkbox(DONT_DELETE_PHOTO_CHECKBOX).click();
        steps.onCabinetFeedsPage().checkbox(DONT_DELETE_SERVICES_CHECKBOX).click();
    }

    @Step("Выбор категории и вида")
    public void chooseCategory(String category, String view) {
        steps.onCabinetFeedsPage().category(category).click();
        steps.onCabinetFeedsPage().category(view).click();
    }
}
