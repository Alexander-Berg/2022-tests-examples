package ru.auto.tests.mobile.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Расширенные фильтры - мото, выбор марки/модели")
@Feature(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingMmmMotoTest {

    public static final String MARK_1 = "BMW";
    public static final String MODEL_1 = "F 650 GS";
    public static final String MODEL_CODE_1 = "f_650_gs";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        String mark = "BMW";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка всех")
    public void shouldSelectMarkFromAll() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allMark(MARK_1).hover();
        basePageSteps.onListingPage().mmmPopup().allMark(MARK_1).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", MARK_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка популярных")
    public void shouldSelectModelFromPopular() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(MARK_1).click();
        basePageSteps.onListingPage().mmmPopup().popularModel(MODEL_1).name().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(MODEL_CODE_1).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", MARK_1, MODEL_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка всех")
    public void shouldSelectModelFromAll() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(MARK_1).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(MODEL_1).name().hover().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(MODEL_CODE_1.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", MARK_1, MODEL_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка популярных")
    public void shouldExcludeMarkFromPopular() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().popularMark(MARK_1).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", MARK_1.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", MARK_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка всех")
    public void shouldExcludeMarkFromAll() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark(MARK_1).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", MARK_1.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", MARK_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        String mark = "Aprilia";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск марки", mark);
        basePageSteps.onListingPage().mmmPopup().marksList().waitUntil(hasSize(1)).get(0).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск модели")
    public void shouldSearchModel() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(MARK_1).click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск модели", MODEL_1);
        basePageSteps.onListingPage().mmmPopup().modelsList().waitUntil(hasSize(1)).get(0).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(MODEL_CODE_1).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", MARK_1, MODEL_1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марок")
    public void shouldMultiSelectMarks() {
        String mark1 = "Ducati";
        String mark2 = "BMW";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideElement(basePageSteps.onListingPage().mmmPopup().applyFiltersButton());
        basePageSteps.onListingPage().mmmPopup().popularMark(mark1).click();
        basePageSteps.showElement(basePageSteps.onListingPage().mmmPopup().applyFiltersButton());
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();

        basePageSteps.onListingPage().filters().button("Ещё марка, модель").click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark2).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL)
                .addParam("catalog_filter", format("mark=%s", mark1.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s", mark2.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения\n%s\nВсе модели / Все поколения", mark1,
                        mark2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор моделей")
    public void shouldMultiSelectModels() {
        String mark = "Aprilia";
        String model1 = "AF1 125";
        String model2 = "Classic 125";
        String modelCode1 = "AF1_125";
        String modelCode2 = "CLASSIC_125";

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(mark.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().allModel(model2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL)
                .addParam("catalog_filter", format("mark=%s,model=%s", mark.toUpperCase(), modelCode1))
                .addParam("catalog_filter", format("mark=%s,model=%s", mark.toUpperCase(), modelCode2))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s, %s", mark, model1, model2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс марки в листинге")
    public void shouldResetMarkInListing() {
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\nВсе модели / Все поколения", MARK_1)))
                .click();
        basePageSteps.onListingPage().filters().mmm().button(format("Марка%s", MARK_1)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Марка и модель"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели в листинге")
    public void shouldResetModelInListing() {
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(MODEL_CODE_1).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\n%s", MARK_1, MODEL_1)));
        basePageSteps.onListingPage().filters().mmm().expandButton().click();
        basePageSteps.onListingPage().filters().mmm().button(format("Модель%s", MODEL_1)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK_1.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", MARK_1)));
    }
}
