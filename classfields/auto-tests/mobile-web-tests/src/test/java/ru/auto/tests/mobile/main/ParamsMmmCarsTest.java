package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - легковые, выбор марки/модели/поколения")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ParamsMmmCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        String mark = "Audi";

        basePageSteps.setWindowMaxHeight();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onMainPage().mmmPopup().popularModels().waitUntil(isDisplayed());
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton("Готово").click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель", mark)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(CARS).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        String mark = "Audi";
        String model = "A3";

        basePageSteps.setWindowMaxHeight();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton("Готово").click();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().popularModel(model).name().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s\nПоколение", mark, model)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s\nПоколение", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        String mark = "Audi";
        String model = "A3";
        String generation = "IV (8Y)";
        String generationCode = "21837610";

        basePageSteps.setWindowMaxHeight();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton("Готово").click();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().popularModel(model).name().click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm().button("Поколение").click();
        basePageSteps.onMainPage().mmmPopup().generation(generation).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s\nПоколение\n%s", mark, model, generation)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(generationCode).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s (%s)", mark, model, generation)));
    }

    @Test
    @DisplayName("Клик по ссылке «Все марки»")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickAllMarksUrl() {
        basePageSteps.onMainPage().paramsPopup().mmmBlock().button("Все марки").hover().click();
        basePageSteps.onMainPage().mmmPopup().popularMarks().waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
    }
}
