package ru.auto.tests.cabinet.start_page;

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
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.START;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_PRICE_LIST;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 2019-02-21
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Стартовая страница. Агентский дилер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class StartPageForAgencyDealerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                        "cabinet/ApiAccessClient",
                        "cabinet/DesktopClientsGet/AgencyDealerNotModerated",
                        "cabinet/CommonCustomerGet",
                        "cabinet/UserOffersAllCount",
                        "cabinet/DealerCampaigns")
                .post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(START).shouldNotSeeDiff();
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Скриншот")
    public void shouldSeeStartPage() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onStartPage().startPage());

        urlSteps.setProduction().open();
        waitSomething(2, TimeUnit.SECONDS);
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onStartPage().startPage());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «калькуляторе»")
    public void shouldSeeCalculatorPage() {
        steps.onStartPage().button("калькуляторе").click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Добавить объявление»")
    public void shouldSeeAddNewOfferForm() {
        steps.onStartPage().button("Добавить объявление").click();
        steps.onStartPage().firstTariff().click();
        steps.switchToNextTab();
        urlSteps.testing().path(BETA).path(CARS).path(NEW).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «правилам»")
    public void shouldSeeSupportPage() {
        steps.onStartPage().button("правилам").click();
        steps.switchToNextTab();
        urlSteps.shouldNotDiffWith(YANDEX_SUPPORT_AUTORU_PRICE_LIST);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «настроить автозагрузку XML»")
    public void shouldSeeUploadPage() {
        steps.onStartPage().button("настроить автозагрузку XML.").click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «Редактировать информацию о салоне»")
    public void shouldSeeCardPage() {
        steps.onStartPage().button("Редактировать информацию о\u00a0салоне").click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «Редактировать объявления»")
    public void shouldSeeListingPage() {
        steps.onStartPage().button("Редактировать объявления").click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Ссылка «Посмотреть список»")
    public void shouldSeeSalePage() {
        steps.onStartPage().button("Посмотреть список").click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).shouldNotSeeDiff();
    }
}
