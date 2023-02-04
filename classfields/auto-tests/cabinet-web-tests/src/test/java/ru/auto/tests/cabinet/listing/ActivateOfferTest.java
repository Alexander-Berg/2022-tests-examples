package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Сниппет активного объявления. Активация оффера")
@Story("Листинг объявлений дилера")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ActivateOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS},
                {TRUCKS},
                {MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/UserOffersCarsUsedInactive"),
                stub("cabinet/UserOffersTrucksUsedInactive"),
                stub("cabinet/UserOffersMotoUsedInactive"),
                stub("cabinet/UserOffersCarsActivate"),
                stub("cabinet/UserOffersTrucksActivate"),
                stub("cabinet/UserOffersMotoActivate")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "inactive").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Активировать»")
    public void shouldSeeActivatedOffer() {
        steps.onCabinetOffersPage().snippet(0).waitUntil(isDisplayed());
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Активировать").click();

        steps.onCabinetOffersPage().notifier().should(isDisplayed()).should(hasText("Объявление активировано"));
    }
}
