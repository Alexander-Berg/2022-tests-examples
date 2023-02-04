package ru.yandex.realty.managementnew.profsearch;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.PROFILTERS;
import static ru.yandex.realty.page.ProfSearchPage.MOSCOW_RGID;
import static ru.yandex.realty.page.ProfSearchPage.RGID;

/**
 * @author kantemirov
 */
@DisplayName("Профпоиск. Фильтры коммерческой недвижимости.")
@Feature(PROFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BuyCommercialProfFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    @ProfsearchAccount
    private Account account;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Офисное помещение", "OFFICE"},
                {"Торговое помещение", "RETAIL"},
                {"Помещение свободного назначения", "FREE_PURPOSE"},
                {"Складское помещение", "WAREHOUSE"},
                {"Производственное помещение", "MANUFACTURING"},
                {"Земельный участок", "LAND"},
                {"Общепит", "PUBLIC_CATERING"},
                {"Автосервис", "AUTO_REPAIR"},
                {"Гостиницу", "HOTEL"},
                {"Готовый бизнес", "BUSINESS"},
                {"Юридический адрес", "LEGAL_ADDRESS"},
        });
    }

    @Before
    public void before() {
        passportSteps.login(account);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID).open();
        user.onProfSearchPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Профпоиск. Параметр типа коммерческой недвижимости")
    public void shouldSeeBuyCommercialType() {
        user.onProfSearchPage().filters().dropDownButton("Купить квартиру").click();
        user.onProfSearchPage().filters().selectPopup().item("Коммерческую недвижимость").click();
        user.onProfSearchPage().filters().selectPopup().selectCheckBox(label);
        user.onProfSearchPage().filters().submitButton().click();
        urlSteps.queryParam("category", "COMMERCIAL").queryParam("commercialType", expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
