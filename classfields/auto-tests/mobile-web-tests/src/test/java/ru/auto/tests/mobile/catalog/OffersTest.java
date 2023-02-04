package ru.auto.tests.mobile.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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

import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - «Предложения о продаже»")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class OffersTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "granta";
    private static final int VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/").open();
        basePageSteps.onCatalogGenerationPage().bodiesList().get(
                basePageSteps.onCatalogGenerationPage().bodiesList().size() - 1).hover();
        basePageSteps.onCatalogModelPage().offers().waitUntil(isDisplayed());
        basePageSteps.onCatalogModelPage().offers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        basePageSteps.onCatalogModelPage().offers().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().title().waitUntil(hasText(containsString(capitalize(MODEL))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Смотреть все»")
    public void shouldClickShowAllButton() {
        basePageSteps.onCatalogModelPage().offers().allButton().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(ALL).shouldNotSeeDiff();
    }
}
