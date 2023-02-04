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
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.AUTO_SNIPPET;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - ссылка на дилера в сниппете объявления")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetDealerTrucksListTest {

    private static final String DEALER_CODE = "agt_treyding_moskva";

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
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SearchTrucksAll")
        ).create();

        urlSteps.testing().path(MOSKVA).path(TRUCK).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение грузового сниппета диллера, тип листинга «Список»")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getSale(5).should(hasText("IVECO EuroCargo\nрефрижератор\nг/п 4.7 т\n" +
                "5.9 л / 252 л.с. / Дизель\n3-х местная с 1 спальным\n4x2\nкрасный\nмеханика\n" +
                "5 250 000 ₽\n2017\n75 676 км\nАГТ-Трейдинг\nМосква"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на страницу дилера, грузовой сниппет, тип листинга «Список»")
    public void shouldClickDealerUrl() {
        basePageSteps.onListingPage().getSale(5).dealerUrl().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();

        urlSteps.testing().path(DILER).path(TRUCK).path(ALL).path(DEALER_CODE).path(SLASH)
                .addParam(FROM, AUTO_SNIPPET).shouldNotSeeDiff();
    }

}
