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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Фильтры - легковые, выбор марки/модели/поколения")
@Feature(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingMmmCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки из списка популярных")
    public void shouldSelectMarkFromPopular() {
        String mark = "Audi";
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).hover();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().popularModel(model).name().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model).name().hover().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularModel(model).arrowButton().click();
        basePageSteps.onListingPage().mmmPopup().popularModel(shield).click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).name().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark("Иномарки").arrowButton().click();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().radioButton("Исключить").click();
        basePageSteps.onListingPage().mmmPopup().allMark(mark).click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск марки", mark.toLowerCase());
        basePageSteps.onListingPage().mmmPopup().marksList().waitUntil(hasSize(1)).get(0).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark).click();
        basePageSteps.onListingPage().mmmPopup().input("Поиск модели", model);
        basePageSteps.onListingPage().mmmPopup().modelsList().waitUntil(hasSize(2)).get(0).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s\nПоколение", mark, model.toUpperCase())));
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

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Поколение").click();
        basePageSteps.onListingPage().mmmPopup().generation(generation).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase())
                .path(generationCode).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(anyOf(
                        hasText(format("%s\n%s (%s)", mark, model, generation)),
                        hasText(format("Марка\n%s\nМодель\n%s\nПоколение\n%s", mark, model, generation))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марок")
    public void shouldMultiSelectMarks() {
        String mark1 = "Audi";
        String mark2 = "BMW";

        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark1).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        basePageSteps.onListingPage().filters().button("Ещё марка, модель").click();
        basePageSteps.onListingPage().mmmPopup().popularMark(mark2).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
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
        String mark = "Audi";
        String model1 = "A3";
        String model2 = "A4";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().allModel(model2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
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
    @DisplayName("Мультивыбор шильдов")
    public void shouldMultiSelectShields() {
        String mark = "Audi";
        String model1 = "A3";
        String model2 = "A5";
        String shield1 = "A3 g-tron";
        String shield2 = "A5 g-tron";
        String shieldCode1 = "g_tron";
        String shieldCode2 = "g_tron";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().allModel(model1).arrowButton().click();
        basePageSteps.onListingPage().mmmPopup().allModel(shield1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().allModel(model2).arrowButton().click();
        basePageSteps.onListingPage().mmmPopup().allModel(shield2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("catalog_filter", format("mark=%s,model=%s,nameplate_name=%s", mark.toUpperCase(),
                        model1.toUpperCase(), shieldCode1))
                .addParam("catalog_filter", format("mark=%s,model=%s,nameplate_name=%s", mark.toUpperCase(),
                        model2.toUpperCase(), shieldCode2))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s, %s\nПоколение", mark, shield2, shield1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор поколений")
    public void shouldMultiSelectGenerations() {
        String mark = "Audi";
        String model = "A3";
        String generation1 = "IV (8Y)";
        String generationCode1 = "21837610";
        String generation2 = "III (8V) Рестайлинг";
        String generationCode2 = "20785010";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Поколение").click();
        basePageSteps.onListingPage().mmmPopup().generation(generation1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().generation(generation2).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model.toUpperCase(), generationCode1))
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model.toUpperCase(), generationCode2))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s (%s, %s)", mark, model, generation1, generation2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколений для разных моделей одной марки")
    public void shouldSelectGenerationsForDifferentModels() {
        String mark = "Audi";
        String model1 = "A3";
        String generation1 = "IV (8Y)";
        String generationCode1 = "21837610";
        String model2 = "A4";
        String generation2 = "V (B9) Рестайлинг";
        String generationCode2 = "21460328";

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("catalog_filter",
                        format("mark=%s,model=%s", mark.toUpperCase(), model1.toUpperCase()))
                .addParam("catalog_filter",
                        format("mark=%s,model=%s", mark.toUpperCase(), model2.toUpperCase())).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button("Поколение").click();
        basePageSteps.onListingPage().mmmPopup().button(model1.toUpperCase()).click();
        basePageSteps.onListingPage().mmmPopup().generation(generation1).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().button(model2.toUpperCase()).click();
        basePageSteps.onListingPage().mmmPopup().generation(generation2).checkbox().click();
        basePageSteps.onListingPage().mmmPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model1.toUpperCase(), generationCode1))
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model2.toUpperCase(), generationCode2))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s (%s), %s (%s)", mark, model1, generation1, model2, generation2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс поколения в поп-апе")
    public void shouldResetGenerationInPopup() {
        String mark = "Audi";
        String model = "A3";
        String generation = "IV (8Y)";
        String generationCode = "21837610";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase())
                .path(generationCode).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().filters().mmm().button(format("Поколение%s", generation)).click();
        basePageSteps.onListingPage().mmmPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onListingPage().mmmPopup().generation(generation).checkboxChecked().waitUntil(isDisplayed());
        basePageSteps.onListingPage().mmmPopup().resetButton().click();
        basePageSteps.onListingPage().mmmPopup().resetButton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().mmmPopup().title().waitUntil(hasText("Выбрать поколения"));
        basePageSteps.onListingPage().mmmPopup().generation(generation).checkboxChecked().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s\nПоколение", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс марки в листинге")
    public void shouldResetMarkInListing() {
        String mark = "Audi";
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\nВсе модели / Все поколения", mark)))
                .click();
        basePageSteps.onListingPage().filters().mmm().button(format("Марка%s", mark)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText("Марка и модель"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели в листинге")
    public void shouldResetModelInListing() {
        String mark = "Audi";
        String model = "A3";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\n%s\nПоколение", mark, model)));
        basePageSteps.onListingPage().filters().mmm().expandButton().click();
        basePageSteps.onListingPage().filters().mmm().button(format("Модель%s", model)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс поколения в листинге")
    public void shouldResetGenerationInListing() {
        String mark = "Audi";
        String model = "A3";
        String generation = "IV (8Y)";
        String generationCode = "21837610";

        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase())
                .path(generationCode).path(ALL).open();
        basePageSteps.onListingPage().filters().mmm().should(hasText(format("%s\n%s (%s)", mark, model, generation)))
                .click();
        basePageSteps.onListingPage().filters().mmm().button(format("Поколение%s", generation)).resetButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s\nПоколение", mark, model)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldCloseMmmPopup() {
        basePageSteps.onListingPage().filters().mmm().click();
        basePageSteps.onListingPage().mmmPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().mmmPopup().closeButton().click();
        basePageSteps.onListingPage().mmmPopup().waitUntil(not(isDisplayed()));
    }
}
