package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.AUTO_SNIPPET;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.TABLE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - ссылка на дилера в сниппете объявления")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetDealerMotoTableTest {

    private static final String DEALER_CODE = "temp_moskva";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchMotoBreadcrumbs"),
                stub("desktop/SearchMotoAll")
        ).create();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).addParam(OUTPUT_TYPE, TABLE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение мото сниппета диллера, тип листинга «Таблица»")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getSale(5).should(hasText("Triumph Street Triple\n340 000 ₽\n32 550 км\n2010\n" +
                "675 см³\nМосква\nТемп"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на страницу дилера, мото сниппет, тип листинга «Таблица»")
    public void shouldClickDealerUrl() {
        basePageSteps.onListingPage().getSale(5).dealerUrl().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();

        urlSteps.testing().path(DILER).path(MOTORCYCLE).path(ALL)
                .path(DEALER_CODE).path(SLASH).addParam(FROM, AUTO_SNIPPET).shouldNotSeeDiff();
    }

}
