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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Regions.DEFAULT_RADIUS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - последние поиски")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class LastSearchesTest {

    private static final String SEARCH = "%s/moskva/cars/toyota/corolla/all/";
    private static final int MAX_LAST_SEARCHES = 9;

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchHistory",
                "desktop/UserFavoritesCarsSubscriptionsToyotaCorollaPost").post();

        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока")
    public void shouldSeeLastSearchesBlock() {
        basePageSteps.onMainPage().lastSearches().searchesList().should(hasSize(MAX_LAST_SEARCHES));
        basePageSteps.onMainPage().lastSearches().should(hasText("Последние поиски\nToyota Corolla\nМосква\nAurus\n" +
                "Москва\nAudi\nМосква\nSuzuki\nМосква\nToyota\nМосква\nVolkswagen\nМосква\nSkoda\nМосква\nPorsche\n" +
                "Москва\nChery\nМосква"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа после сохранения поиска")
    public void shouldSeeLastSearchesPopup() {
        basePageSteps.onMainPage().lastSearches().getSearch(0).saveSearchButton().click();
        basePageSteps.onMainPage().savedSearchesPopup().waitUntil(isDisplayed()).should(hasText("Toyota Corolla\n" +
                "Электронная почта\nПолучать на почту\nУдалить"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по поиску")
    public void shouldClickSearch() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onMainPage().lastSearches().getSearch(0).click();
        urlSteps.fromUri(format(SEARCH, urlSteps.getConfig().getTestingURI()))
                .addParam("geo_radius", DEFAULT_RADIUS).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Сохранить поиск» под незарегом")
    public void shouldClickSaveSearchButtonUnreg() {
        basePageSteps.onMainPage().lastSearches().getSearch(0).saveSearchButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText("Поиск сохранён"));
        basePageSteps.onMainPage().notifier()
                .waitUntil("Плашка «Поиск сохранён» не пропала", not(isDisplayed()), 6);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Сохранить поиск» под зарегом")
    public void shouldClickSaveSearchButtonReg() {
        basePageSteps.onMainPage().lastSearches().getSearch(0).saveSearchButton().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText("Поиск сохранён"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по поиску в поп-апе")
    public void shouldClickSearchInPopup() {
        basePageSteps.onMainPage().lastSearches().getSearch(0).saveSearchButton().click();
        basePageSteps.onMainPage().header().logo().hover();

        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onMainPage().savedSearchesPopup().searchUrl().waitUntil(isDisplayed()).click();
        urlSteps.fromUri(format(SEARCH, urlSteps.getConfig().getTestingURI()))
                .addParam("geo_radius", DEFAULT_RADIUS).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Листание блока кнопками вправо-влево")
    public void shouldSlideSearches() {
        basePageSteps.onMainPage().lastSearches().previousButton().should(not(isDisplayed()));
        basePageSteps.onMainPage().lastSearches().nextButton().should(isDisplayed()).click();

        basePageSteps.onMainPage().lastSearches().previousButton().should(isDisplayed()).click();
        basePageSteps.onMainPage().lastSearches().previousButton().should(not(isDisplayed()));
    }
}