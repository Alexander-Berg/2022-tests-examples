package ru.auto.tests.cabinet.multiposting;

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
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class MultipostingMarkModelFiltersTest {

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
                stub("cabinet/ClientsGetMultipostingEnabled"),
                stub("cabinet/DealerInfoMultipostingEnabled"),
                stub("cabinet/UserOffersCarsCount"),
                stub("cabinet/UserOffersCarsMarkModelsCarsUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsUsedMultipostingMark"),
                stub("cabinet/UserOffersCarsUsedMultipostingMarkModel"),
                stub("cabinet/UserOffersCarsUsedMultipostingMarkModelGeneration")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация по марке")
    public void shouldFilterByMark() {
        String title = steps.onCabinetOffersPage().snippet(0).title().getText();
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Марка", "Audi");

        urlSteps.replaceQuery("mark_model=AUDI&resetSales=false").shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippets().should(hasSize(greaterThan(0)));
        steps.onCabinetOffersPage().snippet(0).should(not(hasText(title)));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация по марке и модели")
    public void shouldFilterByMarkAndModel() {
        String title = steps.onCabinetOffersPage().snippet(0).title().getText();
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Марка", "Audi");
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Модель", "A3");

        urlSteps.replaceQuery("mark_model=AUDI%23A3&resetSales=false").shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippets().should(hasSize(greaterThan(0)));
        steps.onCabinetOffersPage().snippet(0).should(not(hasText(title)));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация по марке, модели и поколению")
    public void shouldFilterByMarkAndModelAndGeneration() {
        String title = steps.onCabinetOffersPage().snippet(0).title().getText();
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Марка", "Audi");
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Модель", "A3");
        steps.onCabinetOffersPage().salesFiltersBlock().selectItem("Поколение", "III (8V) Рестайлинг");

        urlSteps.replaceQuery("mark_model=AUDI%23A3&resetSales=false&super_gen=20785010").shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippets().should(hasSize(greaterThan(0)));
        steps.onCabinetOffersPage().snippet(0).should(not(hasText(title)));
    }
}
