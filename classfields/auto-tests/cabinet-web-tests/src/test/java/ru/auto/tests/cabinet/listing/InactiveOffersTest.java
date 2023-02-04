package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import static org.hamcrest.Matchers.not;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Неактивные объявления")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InactiveOffersTest {

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
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsedInactive"),
                stub("cabinet/UserOffersTrucksUsedInactive"),
                stub("cabinet/UserOffersMotoUsedInactive")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "inactive").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Услуга «Премиум» на неактивном объявлении")
    public void shouldDisablePremiumButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().premium().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().premium().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Услуга «Турбо» на неактивном объявлении")
    public void shouldDisableTurboButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().turbo().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().turbo().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Услуга «Спец» на неактивном объявлении")
    public void shouldDisableSpecButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().special().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().special().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Услуга «Спец» на неактивном объявлении")
    public void shouldDisableStickersButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().stickers().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Услуга «Предложить избранным» на неактивном объявлении")
    public void shouldDisableFavoritesButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @Ignore
    @DisplayName("Услуга «Поднять» на неактивном объявлении")
    public void shouldDisableFreshButtonOnInactiveOffer() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().click();

        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().should(isDisplayed());
        steps.onCabinetOffersPage().notifier().should(not(isDisplayed()));
    }
}
