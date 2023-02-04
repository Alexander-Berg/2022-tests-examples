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
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.PROFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.ROOMS_IN_APART;
import static ru.yandex.realty.page.ProfSearchPage.RGID;

@DisplayName("Профпоиск. Фильтры поиска для аренды комнаты.")
@Feature(PROFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedProfFiltersRentRoomTest {

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
                {"2 комнаты", "2"},
                {"3 комнаты", "3"},
//                {"4 комнаты", "PLUS_4"}
        });
    }

    @Before
    public void before() {
        passportSteps.login(account);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, UrlSteps.SPB_I_LO_RGID)
                .queryParam("type", "RENT").queryParam("category", "ROOMS").open();
        user.onProfSearchPage().openExtFilter();
        user.scrollToElement(user.onProfSearchPage().extendFilters().button(ROOMS_IN_APART));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр количества комнат")
    public void shouldSeeRoomsTotalFiltersInUrl() {
        user.onProfSearchPage().extendFilters().select(ROOMS_IN_APART, label);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("roomsTotal", expected).shouldNotDiffWithWebDriverUrl();
    }
}
