package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.PREMIUM_ACTIVE;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.SPEC_ACTIVE;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.TURBO_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Услуга «Турбо-продажа»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TurboServiceTest {

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
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsProductsTurboPost"),
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersTrucksProductsTurboPost"),
                stub("cabinet/UserOffersMotoUsed"),
                stub("cabinet/UserOffersMotoProductsTurboPost")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "active").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Нотификация с подробным описанием услуги")
    public void shouldSeeTurboNotificationPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().turbo().hover();

        steps.onCabinetOffersPage().popup().should(hasText("Турбо-продажа\n" +
                "Ваше предложение увидит максимум посетителей   это увеличит шансы на быструю и выгодную продажу. " +
                "К объявлению будут применены услуги «Премиум» и «Спецпредложение» на 7 дней, а на 1-й, 3-й и " +
                "5-й день мы поднимем его в поиске.\nПодключить за 2 700 ₽ на 7 дней"));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кнопка «Турбо-продажа» после применения")
    public void shouldSeeTurboButton() {
        steps.applyTurboService(0);

        steps.onCabinetOffersPage().snippet(0).serviceButtons().turbo()
                .should(hasClass(containsString(TURBO_ACTIVE)));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().premium()
                .should(hasClass(containsString(PREMIUM_ACTIVE)));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().special()
                .should(hasClass(containsString(SPEC_ACTIVE)));
    }
}
