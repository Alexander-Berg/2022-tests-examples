package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static com.google.common.collect.Sets.newHashSet;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное развёрнутое объявление")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActiveUnfoldedSaleMotoTest {

    private final static String SALE_ID = "1076842087";
    private final static String SALE_ID_HASH = "1076842087-f1e84";

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
                stub().withGetDeepEquals(USER_OFFERS_MOTO_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.MOTO.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersMotoActive"),
                stub("desktop-lk/UserOffersMotoStats")
        ).create();

        urlSteps.testing().path(MY).path(MOTO).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение раскрытого активного объявления")
    public void shouldSeeUnfoldedSale() {
        String IGNORE = "//div[contains(@class, 'Timer VasDiscount__value')]";

        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onLkSalesPage().getSale(0)
                        .waitUntil(isDisplayed()), newHashSet(IGNORE));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onLkSalesPage().getSale(0)
                        .waitUntil(isDisplayed()), newHashSet(IGNORE));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeeColorPopup() {
        screenshotSteps.setWindowSizeForScreenshot();

        basePageSteps.onLkSalesPage().getSale(0).colorIcon().should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onLkSalesPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onLkSalesPage().activePopupImage()));

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesPage().getSale(0).colorIcon().should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onLkSalesPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onLkSalesPage().activePopupImage()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeeFreshPopup() {
        screenshotSteps.setWindowSizeForScreenshot();

        basePageSteps.onLkSalesPage().getSale(0).freshButton().should(isDisplayed()).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onLkSalesPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onLkSalesPage().activePopupImage()));

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onLkSalesPage().getSale(0).freshButton().should(isDisplayed()).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithCuttingIgnoreAreas(basePageSteps.onLkSalesPage().activePopup()
                        .waitUntil(isDisplayed()), newHashSet(basePageSteps.onLkSalesPage().activePopupImage()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку объявления»")
    public void shouldClickSaleTitle() {
        basePageSteps.onLkSalesPage().getSale(0).title().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path("/ducati/monster_s4/").path(SALE_ID_HASH)
                .path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkSalesPage().getSale(0).button("Редактировать").should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOTO).path(EDIT).path(SALE_ID_HASH).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Договор»")
    public void shouldClickDocButton() {
        basePageSteps.onLkSalesPage().getSale(0).button("Договор купли-продажи").waitUntil(isDisplayed())
                .click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path("/docs/dkp/").addParam("sale_id", SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по столбику на графике")
    public void shouldClickChartItem() {
        basePageSteps.onLkSalesPage().getSale(0).chart().getItem(0).hover();
        basePageSteps.onLkSalesPage().activePopup()
                .waitUntil(hasText("3 звонка\n0 добавили в избранное\n50 просмотров"));
    }

}
