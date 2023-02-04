package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг объявлений - рекомендованные")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RecommendedTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        //mockRule.newMock().with("desktop/SessionAuthUser",
        //        "desktop/SearchCarsBreadcrumbsRid213",
        //        "desktop/PersonalizationGetRecommendedOffers").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam("recommended", "true").open();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Закрываем пресет")
    public void shouldClosePreset() {
        //mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
        // "desktop/ProxyPublicApi").update();

        basePageSteps.onListingPage().presetBar().should(hasText("Вы смотрите подборку «Рекомендованные»Сбросить"));
        basePageSteps.onListingPage().presetBar().button("Сбросить").click();
        basePageSteps.onListingPage().presetBar().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }
}