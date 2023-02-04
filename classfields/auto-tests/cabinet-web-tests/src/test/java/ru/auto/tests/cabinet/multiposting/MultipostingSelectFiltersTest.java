package ru.auto.tests.cabinet.multiposting;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Мультипостинг")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MultipostingSelectFiltersTest {

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
    public String selectName;

    @Parameterized.Parameter(1)
    public String selectItem;

    @Parameterized.Parameter(2)
    public String urlParam;

    @Parameterized.Parameter(3)
    public String query;

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static Object[][] getTestParameters() {
        return new Object[][] {
                {"Сайты", "Авто.ру 1", "tag", "%s=autoru_posted"},
                {"Сайты", "Авито 1", "tag", "%s=avito_posted"},
                {"Сайты", "Дром 1", "tag", "%s=drom_posted"},
                {"С услугами", "Авто.ру ", "multiposting_service",
                        "%1$s=autoru&%1$s=autoru_special&%1$s=autoru_fresh&%1$s=autoru_premium" +
                                "&%1$s=autoru_badge&%1$s=autoru_turbo&resetSales=false"},
                {"С услугами", "\u00a0\u00a0До 2 раз больше просмотров на 1 день 1",
                        "multiposting_service", "%s=avito_x2_1&resetSales=false"},
                {"Проверки по VIN", "Проверено 1", "tag", "%s=vin_resolution_ok&resetSales=false"}
        };
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/ClientsGetMultipostingEnabled"),
                stub("cabinet/DealerInfoMultipostingEnabled"),
                stub("cabinet/UserOffersCarsCount"),
                stub("cabinet/UserOffersCarsUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsUsedMultipostingActiveAutoru"),
                stub("cabinet/UserOffersCarsUsedMultipostingActiveAvito"),
                stub("cabinet/UserOffersCarsUsedMultipostingActiveDrom"),
                stub("cabinet/UserOffersCarsUsedMultipostingActiveAutoruAllServices"),
                stub("cabinet/UserOffersCarsUsedMultipostingActiveAvitoX2_1"),
                stub("cabinet/UserOffersCarsUsedMultipostingVinChecked")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация, селекты")
    public void shouldFilter() {
        String title = steps.onCabinetOffersPage().snippet(0).title().getText();
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem(selectName, selectItem);

        urlSteps.replaceQuery(format(query, urlParam)).shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippets().should(hasSize(greaterThan(0)));
        steps.onCabinetOffersPage().snippet(0).should(not(hasText(title)));
    }
}
