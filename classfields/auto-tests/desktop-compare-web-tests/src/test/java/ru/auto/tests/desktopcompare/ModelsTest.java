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
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение моделей")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ModelsTest {

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
                "desktop/SearchCarsBreadcrumbs",
                "desktop-compare/UserCompareCars",
                "desktop-compare/UserCompareCarsModels").post();

        urlSteps.testing().path(COMPARE_MODELS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение моделей")
    public void shouldSeeModels() {
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
    @DisplayName("Клик по названию модели")
    @Category({Regression.class, Testing.class})
    public void shouldClickModelTitle() {
        basePageSteps.onComparePage().getModel(1).title().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CATALOG).path(CARS).path("/vaz/xray/20497289/20497306/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по фото модели")
    @Category({Regression.class, Testing.class})
    public void shouldClickModelPhoto() {
        basePageSteps.onComparePage().getModel(1).title().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CATALOG).path(CARS).path("/vaz/xray/20497289/20497306/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «N в продаже»")
    @Category({Regression.class, Testing.class})
    public void shouldClickOnSaleUrl() {
        basePageSteps.onComparePage().onSaleUrl("334 в продаже").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path("/vaz/xray/20497289/20497306/20497308/").path(ALL)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление модели из сравнения")
    @Category({Regression.class, Testing.class})
    public void shouldDeleteModelFromCompare() {
        mockRule.with("desktop-compare/UserCompareCarsModelsDelete").update();

        basePageSteps.onComparePage().getModel(1).hover();
        basePageSteps.onComparePage().getModel(1).deleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onComparePage().modelsList().waitUntil(hasSize(1));
        basePageSteps.onComparePage().notifier().waitUntil(isDisplayed())
                .should(hasText("Модель была удалена из сравнения. Вернуть"));
        basePageSteps.onComparePage().getModel(0).title().should(hasText("Kia K5 III"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Плавающая панель")
    @Category({Regression.class, Testing.class})
    public void shouldSeeFloatingPanel() {
        basePageSteps.onComparePage().footer().hover();
        basePageSteps.onComparePage().floatingPanel().waitUntil(isDisplayed())
                .should(hasText("Kia K5 III\nLADA (ВАЗ) XRAY I\nДобавить модель"));
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
    @DisplayName("Переключение на сравнение объявлений")
    @Category({Regression.class, Testing.class})
    public void shouldSwitchCompareType() {
        mockRule.with("desktop-compare/UserCompareCarsOffersEmpty").update();

        basePageSteps.onComparePage().radioButton("Объявления").click();
        urlSteps.testing().path(COMPARE_OFFERS).shouldNotSeeDiff();
        basePageSteps.onComparePage().radioButtonSelected("Объявления").should(isDisplayed());
        basePageSteps.onComparePage().stub().should(hasText("Нажмите кнопку «Добавить в сравнение» на странице " +
                "объявления, в списке избранных объявлений или в результатах поиска\nПоиск авто"));
    }
}