package ru.auto.tests.mobile.searchline;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Строка поиска")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SearchLineTest {

    public static final String MARK = "BMW";

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
        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке очистки поля ввода")
    public void shouldClearSearchLine() {
        basePageSteps.onMainPage().searchLine().click();
        basePageSteps.onMainPage().searchLine().input().sendKeys(MARK);
        basePageSteps.onMainPage().searchLine().clearTextButton().hover().click();
        basePageSteps.onMainPage().searchLine().input().should(hasValue(""));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Всплывающие подсказки поиска")
    public void shouldSeeSuggest() {
        basePageSteps.onMainPage().searchLine().click();
        basePageSteps.onMainPage().searchLine().input().sendKeys(MARK);
        basePageSteps.onMainPage().searchLine().suggest().itemsList().should(hasSize(3));
        basePageSteps.onMainPage().searchLine().suggest().getItem(0).should(hasText(format("%s\nАвтомобили", MARK)));
        basePageSteps.onMainPage().searchLine().suggest().getItem(1).should(hasText(format("%s\nМотоциклы", MARK)));
        basePageSteps.onMainPage().searchLine().suggest().getItem(2).should(hasText(format("%s\nСкутеры", MARK)))
                .click();
        urlSteps.testing().path(MOSKVA).path(SCOOTERS).path(MARK.toLowerCase()).path(ALL).addParam("query", MARK)
                .addParam("from", "searchline").shouldNotSeeDiff();
    }
}
