package ru.auto.tests.desktop.main;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Шапка - мои поиски")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Category({Regression.class})
public class SearchesTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.setWideWindowSize();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeSearchesPopup() {
        basePageSteps.onMainPage().header().savedSearchesButton().click();
        basePageSteps.onMainPage().headerSavedSearchesPopup()
                .should(hasText("Поиски\nСохраняйте поиски, чтобы не пропустить новые предложения."));
    }
}
