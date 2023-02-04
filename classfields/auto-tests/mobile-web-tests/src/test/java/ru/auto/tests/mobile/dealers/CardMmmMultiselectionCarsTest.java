package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_MARK;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_MODEL;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_MODEL_2;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка дилера - легковые, мультивыбор марки/модели/поколения")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardMmmMultiselectionCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марок")
    public void shouldMultiSelectMarks() {
        String mark1 = "BMW";
        String mark2 = "Ford";

        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(mark1).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().button("Ещё марка, модель"));
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().popularMark(mark2).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        urlSteps.addParam("catalog_filter", format("mark=%s", mark1.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s", mark2.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения\n%s\nВсе модели / Все поколения", mark1,
                        mark2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор моделей")
    public void shouldMultiSelectModels() {
        urlSteps.path(DEALERSHIP_MARK.toLowerCase()).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().filters().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().allModel(DEALERSHIP_MODEL).hover();
        basePageSteps.onDealerCardPage().mmmPopup().allModel(DEALERSHIP_MODEL).checkbox().click();
        basePageSteps.onDealerCardPage().mmmPopup().allModel(DEALERSHIP_MODEL_2).hover();
        basePageSteps.onDealerCardPage().mmmPopup().allModel(DEALERSHIP_MODEL_2).checkbox().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED)
                .addParam("catalog_filter", format("mark=%s,model=%s",
                        DEALERSHIP_MARK.toUpperCase(), DEALERSHIP_MODEL.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s,model=%s",
                        DEALERSHIP_MARK.toUpperCase(), DEALERSHIP_MODEL_2.toUpperCase()))
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm().waitUntil(hasText(format(
                "%s\n%s, %s\nПоколение", DEALERSHIP_MARK, DEALERSHIP_MODEL, DEALERSHIP_MODEL_2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор поколений")
    public void shouldMultiSelectGenerations() {
        String mark = "BMW";
        String model = "X5";
        String generation1 = "IV (G05)";
        String generationCode1 = "21307931";
        String generation2 = "III (F15)";
        String generationCode2 = "10382710";

        urlSteps.path(mark.toLowerCase()).path(model.toLowerCase()).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().filters().mmm().button("Поколение").click();
        basePageSteps.onDealerCardPage().mmmPopup().generation(generation1).checkbox().click();
        basePageSteps.onDealerCardPage().mmmPopup().generation(generation2).checkbox().click();
        basePageSteps.onDealerCardPage().mmmPopup().title().waitUntil(hasText("Выбрано 2"));
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED)
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model.toUpperCase(), generationCode1))
                .addParam("catalog_filter", format("mark=%s,model=%s,generation=%s",
                        mark.toUpperCase(), model.toUpperCase(), generationCode2))
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s (%s, %s)", mark, model, generation2, generation1)));
    }
}
