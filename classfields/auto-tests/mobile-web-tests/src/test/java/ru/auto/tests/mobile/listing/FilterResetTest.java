package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - сброс фильтров")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class FilterResetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/SearchCarsAllElectro",
                "desktop/SearchCarsCount",
                "desktop/SearchCarsCountElectro",
                "desktop/SearchCarsCountKmAge",
                "desktop/SearchCarsCountDamageElectro",
                "desktop/SearchCarsCountCustomsElectro").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam("engine_group", "ELECTRO")
                .addParam("km_age_from", "1").open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().footer(),
                0, 0);
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeBlock() {
        basePageSteps.onListingPage().filterResetBlock().title().should(hasText("Измените параметры поиска"));
        basePageSteps.onListingPage().filterResetBlock().subTitle()
                .should(hasText("Возможно, предложения с выбранными параметрами очень редки"));
        basePageSteps.onListingPage().filterResetBlock().resetButton("Двигатель: Электро").should(isDisplayed());
        basePageSteps.onListingPage().filterResetBlock().resetButton("Пробег: от\u00a01\u00a0км")
                .should(isDisplayed());
        basePageSteps.onListingPage().filterResetBlock().resetAllButton().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке сброса фильтра")
    public void shouldClickResetFilterButton() {
        basePageSteps.onListingPage().filterResetBlock().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filterResetBlock().resetButton("Пробег: от\u00a01\u00a0км").resetLink().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path("/engine-electro/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке сброса всех фильтров")
    public void shouldClickResetAllButton() throws InterruptedException {
        basePageSteps.onListingPage().filterResetBlock().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filterResetBlock().resetAllButton().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().footer(),
                0, 0);
        waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onListingPage().filterResetBlock().should(not(isDisplayed()));
    }
}
