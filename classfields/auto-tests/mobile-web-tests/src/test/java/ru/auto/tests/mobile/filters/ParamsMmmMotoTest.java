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
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - мото, выбор марки/модели")
@Feature(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ParamsMmmMotoTest {

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
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        String mark = "BMW";

        basePageSteps.onListingPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onListingPage().mmmPopup().popularModels().waitUntil(isDisplayed());
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton("Готово").click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(mark.toLowerCase()).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель", mark)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        basePageSteps.onListingPage().paramsPopup().waitUntil(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        String mark = "Yamaha";
        String model = "FZ1";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onListingPage().mmmPopup().applyFiltersButton("Готово").click();
        basePageSteps.onListingPage().paramsPopup().mmmBlock().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().mmmPopup().popularModel(model).name().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(mark.toLowerCase()).path(model.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s", mark, model)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().filters().mmm().waitUntil(hasText(format("%s\n%s", mark, model)));
    }

    @Test
    @DisplayName("Клик по ссылке «Все марки»")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickAllMarksUrl() {
        basePageSteps.onListingPage().paramsPopup().mmmBlock().button("Все марки").hover().click();
        basePageSteps.onListingPage().mmmPopup().popularMarks().waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
    }
}
