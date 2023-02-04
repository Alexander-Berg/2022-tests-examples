package ru.auto.tests.bem.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Шапка - меню зарега")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderAuthDropdownTest {

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsRid213").post();

        urlSteps.testing().path(CATALOG).open();
        basePageSteps.onCatalogPage().header().avatar().hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Избранное»")
    public void shouldClickFavoritesUrl() {
        basePageSteps.onCatalogPage().header().authDropdown().button("Избранное").click();
        urlSteps.path(CARS).shouldNotSeeDiff();
        basePageSteps.onCatalogPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().header().authDropdown().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Сохранённые поиски")
    public void shouldClickSavedSearchesUrl() {
        basePageSteps.onCatalogPage().header().authDropdown().button("Сохранённые поиски").click();
        urlSteps.path(CARS).shouldNotSeeDiff();
        basePageSteps.onCatalogPage().headerSavedSearchesPopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().header().authDropdown().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Выход»")
    public void shouldLogout() {
        mockRule.overwriteStub(0, "desktop/SessionUnauth");

        basePageSteps.onCatalogPage().header().authDropdown().button("Выйти").click();
        urlSteps.path(CARS).shouldNotSeeDiff();
        basePageSteps.onCatalogPage().header().avatar().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogPage().header().button("Войти").waitUntil(isDisplayed());
    }
}