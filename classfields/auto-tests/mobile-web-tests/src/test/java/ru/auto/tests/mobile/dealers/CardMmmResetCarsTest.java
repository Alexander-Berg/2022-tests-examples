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
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_GEN;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_GEN_CODE;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_MARK;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_MODEL;
import static ru.auto.tests.desktop.mobile.page.DealerCardPage.DEALERSHIP_USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - легковые, сброс марки/модели/поколения")
@Epic(DEALERS)
@Feature(DEALER_CARD)
@Story(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardMmmResetCarsTest {

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
    @DisplayName("Сброс поколения в поп-апе")
    public void shouldResetGenerationInPopup() {
        urlSteps.path(DEALERSHIP_MARK.toLowerCase()).path(DEALERSHIP_MODEL.toLowerCase()).path(DEALERSHIP_GEN_CODE).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm());
        basePageSteps.onDealerCardPage().filters().mmm().button(format("Поколение%s", DEALERSHIP_GEN)).click();
        basePageSteps.onDealerCardPage().mmmPopup().title().waitUntil(hasText("Выбрано 1"));
        basePageSteps.onDealerCardPage().mmmPopup().generation(DEALERSHIP_GEN).checkboxChecked().waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().mmmPopup().resetButton().click();
        basePageSteps.onDealerCardPage().mmmPopup().resetButton().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().mmmPopup().title().waitUntil(hasText("Выбрать поколения"));
        basePageSteps.onDealerCardPage().mmmPopup().generation(DEALERSHIP_GEN).checkboxChecked().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();

        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED).path(DEALERSHIP_MARK.toLowerCase())
                .path(DEALERSHIP_MODEL.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s\nПоколение", DEALERSHIP_MARK, DEALERSHIP_MODEL)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс марки в листинге")
    public void shouldResetMarkInListing() {
        urlSteps.path(DEALERSHIP_MARK.toLowerCase()).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm()
                .should(hasText(format("%s\nВсе модели / Все поколения", DEALERSHIP_MARK))));
        basePageSteps.onDealerCardPage().filters().mmm().button(format("Марка%s", DEALERSHIP_MARK)).resetButton().click();

        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm().waitUntil(hasText("Марка и модель"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс модели в листинге")
    public void shouldResetModelInListing() {
        urlSteps.path(DEALERSHIP_MARK.toLowerCase()).path(DEALERSHIP_MODEL.toLowerCase()).open();
        basePageSteps.onDealerCardPage().filters().mmm().should(hasText(format(
                "%s\n%s\nПоколение", DEALERSHIP_MARK, DEALERSHIP_MODEL)));
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm().expandButton());
        basePageSteps.onDealerCardPage().filters().mmm().button(format(
                "Модель%s", DEALERSHIP_MODEL)).resetButton().click();

        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED).path(DEALERSHIP_MARK.toLowerCase())
                .path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", DEALERSHIP_MARK)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс поколения в листинге")
    public void shouldResetGenerationInListing() {
        urlSteps.path(DEALERSHIP_MARK.toLowerCase()).path(DEALERSHIP_MODEL.toLowerCase()).path(DEALERSHIP_GEN_CODE).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().mmm()
                .should(hasText(format("%s\n%s (%s)", DEALERSHIP_MARK, DEALERSHIP_MODEL, DEALERSHIP_GEN))));
        basePageSteps.onDealerCardPage().filters().mmm().button(format("Поколение%s", DEALERSHIP_GEN)).resetButton().click();
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALERSHIP_USED).path(DEALERSHIP_MARK.toLowerCase()).path(SLASH)
                .path(DEALERSHIP_MODEL.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().mmm().waitUntil(hasText(format(
                "%s\n%s\nПоколение", DEALERSHIP_MARK, DEALERSHIP_MODEL)));
    }
}
