package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOfferStats.userOfferStatsForLastDays;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отображение услуг - графики")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DynamicGraphicsByOptionsTest {

    private static final String ID = "1076842087-f1e84";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/UserOffersCarsActive"),
                stub().withGetDeepEquals(format("%s/%s/stats", USER_OFFERS_CARS, ID))
                        .withResponseBody(userOfferStatsForLastDays(ID).getBody()),
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.setWindowMaxHeight();
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Изменение графиков по ховеру на выделение цветом")
    public void shouldChangeGraphicsByColor() {
        basePageSteps.onLkSalesPage().getSale(0).colorIcon().should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesPage().getSale(0).colorIcon().should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Изменение графиков по ховеру на поднятие в ТОП")
    public void shouldChangeGraphicsByTop() {
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Поднятие в ТОП").hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Поднятие в ТОП").hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Изменение графиков по ховеру на спецпредложение")
    public void shouldChangeGraphicsBySpec() {
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Спецпредложение").hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).should(isDisplayed()).click();
        basePageSteps.onLkSalesPage().getSale(0).vas().option("Спецпредложение").hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0).chart());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

}
