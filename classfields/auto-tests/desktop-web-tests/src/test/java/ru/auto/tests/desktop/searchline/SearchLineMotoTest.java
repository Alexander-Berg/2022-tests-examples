package ru.auto.tests.desktop.searchline;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SEARCHLINE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Строка поиска в разных категориях")
@Feature(SEARCHLINE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SearchLineMotoTest {

    private static final String SUGGEST_TEXT = "Мотоциклы";
    private static final String SEARCH_TEXT = "Новые";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchMotoBreadcrumbs",
                "desktop/SearchMotoAll",
                "desktop/SearchlineSuggestMoto").post();

        urlSteps.testing().path(MOTORCYCLE).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Всплывающие подсказки поиска")
    public void shouldSeeSuggest() {
        basePageSteps.onListingPage().header().searchLine().input("Поиск по объявлениям", SEARCH_TEXT);
        basePageSteps.onListingPage().header().searchLine().suggest().getItem(0).waitUntil(isDisplayed())
                .should(hasText(String.format("%s\n%s", SUGGEST_TEXT, SEARCH_TEXT)));
    }
}