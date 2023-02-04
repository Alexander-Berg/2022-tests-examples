package ru.auto.tests.cabinet.backonsale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.BACK_ON_SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockComeback.comebackExampleOneOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.COMEBACK;
import static ru.auto.tests.desktop.step.CookieSteps.IS_SHOWING_BACK_ON_SALE_PLACEHOLDER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Кабинет дилера. Снова в продаже. Пустой список")
@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.BACK_ON_SALE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ListingEmptyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub().withPostDeepEquals(COMEBACK)
                        .withResponseBody(comebackExampleOneOffer().setNoOffers().getBody())
        ).create();

        cookieSteps.setCookieForBaseDomain(IS_SHOWING_BACK_ON_SALE_PLACEHOLDER, "1");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(BACK_ON_SALE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Пустой листинг")
    public void shouldSeeEmptyListing() {
        basePageSteps.onCabinetOnSaleAgainPage().listing().should(hasText("Объявления не найдены"));
        basePageSteps.onCabinetOnSaleAgainPage().listing().items().should(hasSize(0));
        basePageSteps.onCabinetOnSaleAgainPage().pager().should(not(isDisplayed()));
    }

}
