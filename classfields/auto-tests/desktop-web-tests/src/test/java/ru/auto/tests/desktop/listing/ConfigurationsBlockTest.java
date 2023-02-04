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
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - блок «Конфигурации»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ConfigurationsBlockTest {

    private static final String MARK = "/kia/";
    private static final String MODEL = "/optima/";

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
                stub("desktop/SearchCarsBreadcrumbsKiaOptima"),
                stub("desktop/SearchCarsAllKiaOptimaShort"),
                stub("desktop/SearchCarsCrosslinksCount"),
                stub("desktop/SearchCarsKiaOptimaConfigurationTechparam")
        ).create();

        urlSteps.testing().path(CARS).path(MARK).path(MODEL).path(ALL).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeConfigurationsBlock() {
        basePageSteps.onListingPage().configurationsList().waitUntil(isDisplayed());
        basePageSteps.onListingPage().configurationsList().should(hasText("Конфигурации Kia Optima\nКомплектация\n" +
                "Модификация\nКоробка\nПривод\nРасход, л\nЦена, ₽\nНа Авто.ру\nComfort\n2.0 л 150 л.с. бензин\nседан\n" +
                "автомат\nпередний\n7.8\n990 000 – 3 500 000\n101 предложение\nPremium\n2.0 л 150 л.с. бензин\n" +
                "седан\nавтомат\nпередний\n7.8\n1 420 000 – 2 050 000\n15 предложений\nPrestige\n" +
                "2.0 л 150 л.с. бензин\nседан\nавтомат\nпередний\n7.8\n1 250 000 – 3 000 000\n28 " +
                "предложений\nВсе конфигурации"));
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке комплектации")
    public void shouldClickConfiguration() {
        String complectationUrl = "/21342050/21342121/21342125/all/";

        mockRule.setStubs(
                stub("desktop/ProxyPublicApi")).update();

        basePageSteps.onListingPage().configurationsList().configurationOffersButton()
                .should(hasText("101 предложение")).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(complectationUrl)
                .addParam("complectationId", "21342344").shouldNotSeeDiff();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Жмем кнопку «Все конфигурации»")
    public void shouldClickAllConfiguration() {
        basePageSteps.onListingPage().configurationsList().configurationButton().should(hasText("Все конфигурации")).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).shouldNotSeeDiff();
    }

}
