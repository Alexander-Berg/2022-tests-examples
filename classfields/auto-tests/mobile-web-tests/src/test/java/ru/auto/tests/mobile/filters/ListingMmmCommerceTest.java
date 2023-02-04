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
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Расширенные фильтры - коммерческие, выбор марки/модели")
@Feature(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingMmmCommerceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        String mark = "Ford";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка всех")
    public void shouldSelectMarkFromAll() {
        String mark = "Ford";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).hover();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка популярных")
    public void shouldSelectModelFromPopular() {
        String mark = "Ford";
        String model = "Transit";
        String modelCode = "transit_lt";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().popularModel(model).name().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(modelCode).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели из списка всех")
    public void shouldSelectModelFromAll() {
        String mark = "Ford";
        String model = "Aerostar";
        String modelCode = "aerostar";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model).name().hover().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(modelCode.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка популярных")
    public void shouldExcludeMarkFromPopular() {
        String mark = "Ford";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", mark.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Исключение марки из списка всех")
    public void shouldExcludeMarkFromAll() {
        String mark = "Asia";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL)
                .addParam("exclude_catalog_filter", format("mark=%s", mark.toUpperCase())).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Исключить %s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        String mark = "Bedford";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск марки", mark.toLowerCase());
        basePageSteps.onListingPage().mmmPopup().marksList().waitUntil(hasSize(1)).get(0).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск модели")
    public void shouldSearchModel() {
        String mark = "Ford";
        String model = "Aerostar";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск модели", model);
        basePageSteps.onListingPage().mmmPopup().modelsList().waitUntil(hasSize(1)).get(0).click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марок")
    public void shouldMultiSelectMarks() {
        String mark1 = "Ford";
        String mark2 = "BAW";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark1).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        basePageSteps.onListingPage().filters().button("Ещё марка, модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark2).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL)
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
        String mark = "Ford";
        String model1 = "Aerostar";
        String modelCode1 = "AEROSTAR";
        String model2 = "Courier";
        String modelCode2 = "COURIER";

        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().allModel(model2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL)
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
        String mark = "Ford";

        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\nВсе модели / Все поколения", mark)))
                .click();
        basePageSteps.onListingPage().filters().mmm().button(format("Марка%s", mark)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Марка и модель"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели в листинге")
    public void shouldResetModelInListing() {
        String mark = "Ford";
        String model = "Aerostar";

        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\n%s", mark, model)));
        basePageSteps.onListingPage().filters().mmm().expandButton().click();
        basePageSteps.onListingPage().filters().mmm().button(format("Модель%s", model)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(LCV).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }
}
