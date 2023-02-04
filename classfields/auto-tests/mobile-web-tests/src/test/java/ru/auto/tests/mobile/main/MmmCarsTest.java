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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mobile.element.Filters.MARK_MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Фильтры - легковые, выбор марки/модели/поколения")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class MmmCarsTest {

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
        basePageSteps.onMainPage().filters().button(MARK_MODEL).click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        String mark = "Audi";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка всех")
    public void shouldSelectMarkFromAll() {
        String mark = "Acura";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).hover();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка популярных")
    public void shouldSelectModelFromPopular() {
        String mark = "Audi";
        String model = "A3";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onMainPage().mmmPopup().popularModel(model).name().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s\nПоколение", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка всех")
    public void shouldSelectModelFromAll() {
        String mark = "Audi";
        String model = "A4";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().allModel(model).name().hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s\nПоколение", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор шильда")
    public void shouldSelectShield() {
        String mark = "Audi";
        String model = "A3";
        String shield = "A3 e-tron";
        String shieldCode = "a3-e_tron";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().popularModel(model).arrowButton().click();
        basePageSteps.onMainPage().mmmPopup().popularModel(shield).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(shieldCode).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s\nПоколение", mark, shield)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка популярных")
    public void shouldExcludeMarkFromPopular() {
        String mark = "Audi";

        basePageSteps.onMainPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", mark.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка всех")
    public void shouldExcludeMarkFromAll() {
        String mark = "Audi";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", mark.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение иномарок")
    public void shouldExcludeForeignMarks() {
        String mark = "Иномарки";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).name().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", "vendor=VENDOR2").shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Исключить иномарки"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение китайских")
    public void shouldExcludeChineseMarks() {
        String mark = "Китайские";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onMainPage().mmmPopup().allMark("Иномарки").arrowButton().click();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", "vendor=VENDOR10").shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Исключить китайские"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение отчественных")
    public void shouldExcludeDomesticMarks() {
        String mark = "Отечественные";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onMainPage().mmmPopup().allMark(mark).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("exclude_catalog_filter", "vendor=VENDOR1").shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Исключить отечественные"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        String mark = "Audi";

        basePageSteps.onMainPage().mmmPopup().input("Поиск марки", mark.toLowerCase());
        basePageSteps.onMainPage().mmmPopup().marksList().waitUntil(hasSize(1)).get(0).click();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск модели")
    public void shouldSearchModel() {
        String mark = "Audi";
        String model = "a3";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onMainPage().mmmPopup().input("Поиск модели", model);
        basePageSteps.onMainPage().mmmPopup().modelsList().waitUntil(hasSize(2)).get(0).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s\nПоколение", mark, model.toUpperCase())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор моделей")
    public void shouldMultiSelectModels() {
        String mark = "Audi";
        String model1 = "A3";
        String model2 = "A4";

        basePageSteps.onMainPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().allModel(model1).checkbox().click();
        basePageSteps.onMainPage().mmmPopup().allModel(model2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("catalog_filter", format("mark=%s,model=%s", mark.toUpperCase(), model1.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s,model=%s", mark.toUpperCase(), model2.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s, %s\nПоколение", mark, model1, model2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldCloseMmmPopup() {
        basePageSteps.onMainPage().mmmPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().mmmPopup().closeButton().click();
        basePageSteps.onMainPage().mmmPopup().waitUntil(not(isDisplayed()));
    }
}
