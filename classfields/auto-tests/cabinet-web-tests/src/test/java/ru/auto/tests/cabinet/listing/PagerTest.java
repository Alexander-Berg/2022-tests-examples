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
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Пагинация")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PagerTest {

    private static final String PREVIOUS = "Предыдущая";
    private static final String NEXT = "Следующая";
    private static final String SECOND_PAGE_NUM = "2";
    private static final String FIRST_PAGE_NUM = "1";

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
                stub("cabinet/UserOffersCarsUsedPageCount2"),
                stub("cabinet/UserOffersCarsUsedPage2")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).addParam(STATUS, "active").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Переход на страницу «2»")
    public void shouldBeOfSecondPage() {
        steps.onCabinetOffersPage().pager().page(SECOND_PAGE_NUM).click();

        urlSteps.addParam("p", SECOND_PAGE_NUM).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Переход на страницу «1» обратно со страницы «2»")
    public void shouldBeReturnedToFirstPageFromSecondPage() {
        steps.onCabinetOffersPage().pager().page(SECOND_PAGE_NUM).click();
        urlSteps.addParam("p", SECOND_PAGE_NUM).shouldNotSeeDiff();

        steps.onCabinetOffersPage().pager().page(FIRST_PAGE_NUM).click();
        urlSteps.replaceParam("p", FIRST_PAGE_NUM).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Следующая»")
    public void shouldBeOfSecondPageWithNextPage() {
        steps.onCabinetOffersPage().pager().buttonContains(NEXT).click();
        urlSteps.addParam("p", SECOND_PAGE_NUM).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Предыдущая»")
    public void shouldBeOnFirstPageAgain() {
        steps.onCabinetOffersPage().pager().buttonContains(NEXT).click();
        urlSteps.addParam("p", SECOND_PAGE_NUM).shouldNotSeeDiff();

        steps.onCabinetOffersPage().pager().buttonContains(PREVIOUS).click();
        urlSteps.replaceParam("p", FIRST_PAGE_NUM).shouldNotSeeDiff();
    }
}
