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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сортировка офферов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SortingOffersTest {

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


    private UrlSteps baseUrl;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerInfoMultipostingDisabled")
        ).create();

        baseUrl = urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active")
                .addParam("sort_dir", "asc");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Сортировка по Цене")
    public void shouldOffersSortedByPrice() {
        mockRule.setStubs(stub("cabinet/UserOffersCars/ActiveSortedPriceAsc")).update();

        baseUrl.addParam("sort", "price").open();

        steps.onCabinetOffersPage().snippet(0).priceBlock().price().should(hasText("185 855 \u20BD"));
        steps.onCabinetOffersPage().snippet(1).priceBlock().price().should(hasText("320 000 \u20BD"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Сортировка по Году")
    public void shouldOffersSortedByYear() {
        mockRule.setStubs(stub("cabinet/UserOffersCars/ActiveSortedYearAsc")).update();

        baseUrl.addParam("sort", "year").open();

        steps.onCabinetOffersPage().snippet(0).title().should(hasText("Citroen C5 II, 2011"));
        steps.onCabinetOffersPage().snippet(1).title().should(hasText("Citroen C5 II, 2012"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Сортировка по Дате размещения")
    public void shouldOffersSortedDate() {
        mockRule.setStubs(stub("cabinet/UserOffersCars/ActiveSortedDateCreatedAsc")).update();

        baseUrl.addParam("sort", "cr_date").open();

        steps.onCabinetOffersPage().snippet(0).date().should(hasText("В продаже802дня"));
        steps.onCabinetOffersPage().snippet(1).date().should(hasText("В продаже788дней"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Сортировка по Марке, модели")
    public void shouldOffersSortedByModel() {
        mockRule.setStubs(stub("cabinet/UserOffersCars/ActiveSortedAlphabetAsc")).update();

        baseUrl.addParam("sort", "alphabet").open();

        steps.onCabinetOffersPage().snippet(0).title().should(hasText("BMW 5 серия 530d xDrive VII (G30/G31), 2018"));
        steps.onCabinetOffersPage().snippet(1).title().should(hasText("BMW 7 серия 750Li xDrive VI (G11/G12), 2016"));
    }
}
