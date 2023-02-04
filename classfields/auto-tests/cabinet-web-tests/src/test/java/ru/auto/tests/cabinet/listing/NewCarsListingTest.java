package ru.auto.tests.cabinet.listing;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.ACTIVE;
import static ru.auto.tests.desktop.consts.QueryParams.AVAILABILITY;
import static ru.auto.tests.desktop.consts.QueryParams.IN_TRANSIT;
import static ru.auto.tests.desktop.consts.QueryParams.ON_ORDER;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class NewCarsListingTest {

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsNew"),
                stub("cabinet/UserOffersCarsNewInTransit")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(NEW).addParam(STATUS, ACTIVE).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Сниппет активного объявления в категории «Легковые новые»")
    public void shouldSeeNewCarOffer() {
        steps.onCabinetOffersPage().snippet(0).should(hasText("360°\nНе было звонков 10 дней\nДоставка в города\nKia Rio IV, " +
                "2017\nClassic Audio\n1.4 MT (100 л.с.)\nZ6FAXXESMAES15626\nПоказать всё\nВ наличии\n500 000 ₽\n" +
                "В продажедней\n8\n0\u2009/\u20090.0%\n0\u2009/\u20090.0%\n0\nПодробнее\nПоднятие\n250 ₽ в день\n" +
                "Премиум\n500 ₽ за день"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Поиск по статусу «В пути»")
    public void shouldFindByInTransit() {
        steps.onCabinetOffersPage().salesFiltersBlock().button("Все параметры").click();
        steps.onCabinetOffersPage().salesFiltersBlock().button("В пути").click();

        urlSteps.addParam(AVAILABILITY, IN_TRANSIT).addParam(AVAILABILITY, ON_ORDER).shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippet(0).availability().should(hasText("В пути"));
    }
}
