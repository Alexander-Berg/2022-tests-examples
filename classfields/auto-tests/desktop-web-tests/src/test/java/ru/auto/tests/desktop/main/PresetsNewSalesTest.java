package ru.auto.tests.desktop.main;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

@DisplayName("Главная - пресеты - «Новые со скидкой»")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PresetsNewSalesTest {

    private static final int PRESET_SALES_CNT = 40;
    private static final int PRESET_VISIBLE_SALES_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onMainPage().presets().salesList().waitUntil(hasSize(PRESET_SALES_CNT));
        basePageSteps.onMainPage().presets().salesList().subList(0, PRESET_VISIBLE_SALES_CNT)
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onMainPage().presets().getSale(0).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().breadcrumbs().waitUntil(isDisplayed())
                .should(hasText(containsString("Продажа автомобилейНовые")));
        basePageSteps.onCardPage().discounts().should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по ссылке «Смотреть все»")
    public void shouldClickShowAllUrl() {
        basePageSteps.onMainPage().presets().showAllUrl().waitUntil(isDisplayed()).hover().click();
        urlSteps.path(CARS).path(NEW).fragment("priceRange").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Скролл пресета")
    public void shouldScrollPreset() {
        basePageSteps.onMainPage().presets().prevButton().should(not(isDisplayed()));
        basePageSteps.onMainPage().presets().nextButton().hover().click();
        basePageSteps.onMainPage().presets().salesList().subList(0, 3)
                .forEach(item -> item.waitUntil(isDisplayed()));
        basePageSteps.onMainPage().presets().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().presets().salesList().subList(0, 3)
                .forEach(item -> item.waitUntil(isDisplayed()));
        basePageSteps.onMainPage().presets().nextButton().waitUntil(isDisplayed());
        basePageSteps.onMainPage().presets().prevButton().waitUntil(not(isDisplayed()));
    }
}
