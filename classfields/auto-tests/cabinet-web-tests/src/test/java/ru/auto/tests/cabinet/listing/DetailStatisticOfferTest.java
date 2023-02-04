package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Pages.OFFER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 19.11.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Детальная статистика по объявлению")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DetailStatisticOfferTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {TRUCKS},
                {MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerInfoMultipostingDisabled",
                "cabinet/ClientsGet",
                "cabinet/UserOffersCarsId",
                "cabinet/UserOffersTrucksId",
                "cabinet/UserOffersMotoId").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(OFFER).path(category).path(SALE_ID).open();
        steps.onCabinetOffersPage().offerstat().getGraphic(0).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Блок с основной информацией по объявлению. Скриншот")
    public void shouldSeeBlockSaleInfo() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().blockSaleInfo());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().blockSaleInfo());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График просмотров объявления. Скриншот")
    public void shouldSeeGraphViewOffer() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(0));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График просмотров объявления. Поп-ап. Скриншот")
    public void shouldSeeGraphViewOfferPopup() {
        steps.moveCursor(steps.onCabinetOffersPage().offerstat().getGraphic(0));
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(0).popupPoint());

        urlSteps.setProduction().open();
        steps.moveCursor(steps.onCabinetOffersPage().offerstat().getGraphic(0));
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(0).popupPoint());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График просмотров телефонов. Скриншот")
    public void shouldSeeGraphViewPhone() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(1));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(1));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График просмотров телефонов. Поп-ап. Скриншот")
    public void shouldSeeGraphViewPhonePopup() {
        steps.moveCursor(steps.onCabinetOffersPage().offerstat().getGraphic(1));
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(1).popupPoint());

        urlSteps.setProduction().open();
        steps.moveCursor(steps.onCabinetOffersPage().offerstat().getGraphic(1));
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(1).popupPoint());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График спецпредложений. Скриншот")
    public void shouldSeeGraphSpecial() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(2));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(2));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("График расходов. Скриншот")
    public void shouldSeeGraphViewMoney() {
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(3));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().offerstat().getGraphic(3));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
