package ru.auto.tests.desktopcompare;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение объявлений")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SalesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-compare/UserCompareCarsOffers").post();

        urlSteps.testing().path(COMPARE_OFFERS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение объявлений")
    public void shouldSeeSales() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onComparePage().content());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onComparePage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по названию объявления")
    @Category({Regression.class, Testing.class})
    public void shouldClickSaleTitle() {
        basePageSteps.onComparePage().getSale(0).title().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/bmw/x7/21406262/21738411/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по фото объявления")
    @Category({Regression.class, Testing.class})
    public void shouldClickSalePhoto() {
        basePageSteps.onComparePage().getSale(0).photo().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/bmw/x7/21406262/21738411/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Показать телефон»")
    @Category({Regression.class, Testing.class})
    public void shouldClickShoPhoneButton() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onComparePage().getSale(0).button("Показать телефон").click();
        basePageSteps.onComparePage().getSale(0).phone().waitUntil(hasText("+7 916 039-84-27"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Объявления»")
    @Category({Regression.class, Testing.class})
    public void shouldClickSalesUrl() {
        basePageSteps.onComparePage().salesUrl("1").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path("/bmw/x7/").path(NEW)
                .addParam("with_discount", "true").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление объявления из сравнения")
    @Category({Regression.class})
    public void shouldDeleteSaleFromCompare() {
        mockRule.with("desktop-compare/UserCompareCarsFavoriteDelete").update();

        basePageSteps.onComparePage().getSale(0).hover();
        basePageSteps.onComparePage().getSale(0).deleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().salesList().waitUntil(hasSize(1));
        basePageSteps.onComparePage().notifier().waitUntil(isDisplayed())
                .should(hasText("Объявление было удалено из сравнения. Вернуть"));
        basePageSteps.onComparePage().getSale(0).title().should(hasText("Nissan Almera Classic I, 2010"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Возвращение удалённого из сравнения объявления")
    @Category({Regression.class, Testing.class})
    public void shouldRestoreSale() {
        mockRule.with("desktop-compare/UserCompareCarsFavoriteDelete",
                "desktop-compare/UserCompareCarsFavoritePost").update();

        basePageSteps.onComparePage().getSale(0).hover();
        basePageSteps.onComparePage().getSale(0).deleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().notifier().waitUntil(isDisplayed());
        basePageSteps.onComparePage().notifier().button(" Вернуть").click();
        basePageSteps.onComparePage().salesList().waitUntil(hasSize(2));
        basePageSteps.onComparePage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onComparePage().getSale(0).title().should(hasText("Nissan Almera Classic I, 2010"));
        basePageSteps.onComparePage().getSale(1).title().should(hasText("BMW X7 I (G07), 2020"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Плавающая панель")
    @Category({Regression.class, Testing.class})
    public void shouldSeeFloatingPanel() {
        basePageSteps.onComparePage().footer().hover();
        basePageSteps.onComparePage().floatingPanel().waitUntil(isDisplayed())
                .should(hasText("7 510 000 ₽\nBMW X7 I (G07), 2020\n400 000 ₽\nNissan Almera Classic I, 2010"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Чекбокс «Показать только отличия»")
    @Category({Regression.class, Testing.class, Screenshooter.class})
    public void shouldClickOnlyDiffCheckbox() {
        screenshotSteps.setWindowSizeForScreenshot();

        basePageSteps.onComparePage().checkbox("Показать только отличия").click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onComparePage().content());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onComparePage().checkbox("Показать только отличия").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onComparePage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение на сравнение моделей")
    @Category({Regression.class, Testing.class})
    public void shouldSwitchCompareType() {
        mockRule.with("desktop-compare/UserCompareCarsModelsEmpty").update();

        basePageSteps.onComparePage().radioButton("Модели").click();
        urlSteps.testing().path(COMPARE_MODELS).shouldNotSeeDiff();
        basePageSteps.onComparePage().radioButtonSelected("Модели").should(isDisplayed());
        basePageSteps.onComparePage().stub().should(hasText("Нажмите кнопку «Добавить модель», чтобы выбрать модели " +
                "и сравнить их\nДобавить модель"));
    }
}