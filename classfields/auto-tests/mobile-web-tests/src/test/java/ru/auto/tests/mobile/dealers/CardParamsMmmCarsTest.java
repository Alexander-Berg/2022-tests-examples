package ru.auto.tests.mobile.dealers;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - параметры - легковые, выбор марки/модели/поколения")
@Feature(AutoruFeatures.FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class CardParamsMmmCarsTest {

    private static final String DEALER_CODE = "/rolf_ugo_vostok_avtomobili_s_probegom_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path(DEALER_CODE).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().paramsButton());
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.setWindowHeight(1500);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        String mark = "Audi";

        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onDealerCardPage().mmmPopup().popularModels().waitUntil(isDisplayed());
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton("Готово").click();
        urlSteps.path(mark.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель", mark)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("%s\nВсе модели / Все поколения", mark)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        String mark = "Audi";
        String model = "A6";

        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().hover();
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().mark(mark).click();
        basePageSteps.onDealerCardPage().mmmPopup().applyFiltersButton().click();
        basePageSteps.scrollDown(200);
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().mmm().button("Модель").click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onDealerCardPage().mmmPopup().popularModel(model).hover();
        basePageSteps.onDealerCardPage().mmmPopup().popularModel(model).name().click();
        urlSteps.path(mark.toLowerCase()).path(model.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().mmm()
                .waitUntil(hasText(format("Марка\n%s\nМодель\n%s\nПоколение", mark, model)));
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().paramsPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onDealerCardPage().filters().mmm()
                .waitUntil(hasText(format("%s\n%s\nПоколение", mark, model)));
    }

    @Test
    @DisplayName("Клик по ссылке «Все марки»")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickAllMarksUrl() {
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().hover();
        basePageSteps.onDealerCardPage().paramsPopup().mmmBlock().button("Все марки").hover().click();
        basePageSteps.onDealerCardPage().mmmPopup().popularMarks().waitUntil(isDisplayed());
        urlSteps.shouldNotSeeDiff();
    }
}
