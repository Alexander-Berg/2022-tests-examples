package ru.auto.tests.cabinet.header;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка. Добавить объявление")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddOfferButtonTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String offerType;

    @Parameterized.Parameter(1)
    public String expectedPath;

    @Parameterized.Parameters(name = "{index}: Добавить объявление «{0}»")
    public static Object[][] getTestParameters() {
        return new Object[][]{
                {"Легковые новые", "/beta/cars/new/"},
                {"Легковые с пробегом", "/beta/cars/used/"},
                {"Мото", MOTO},
                {"Коммерческие", TRUCKS},
        };
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/UserOffersCarsUsed",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerCampaigns",
                "cabinet/DealerInfoMultipostingDisabled").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().header().addOffer().click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    public void shouldSeeAddNewCarsPage() {
        steps.onCabinetOffersPage().suggestCategoryItem(offerType).click();
        urlSteps.testing().path(expectedPath).path(ADD).shouldNotSeeDiff();
    }
}
