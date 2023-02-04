package ru.yandex.realty.managementnew.profsearch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.PROFILTERS;
import static ru.yandex.realty.page.ProfSearchPage.MOSCOW_RGID;
import static ru.yandex.realty.page.ProfSearchPage.RGID;

@DisplayName("Профпоиск. Расширенные фильтры поиска по объявлениям.")
@Feature(PROFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class ExtendedProfFiltersFloorFiltersTest {

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

    @Before
    public void openSaleAdsPage() {
        passportSteps.login(account);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID).open();
        user.onProfSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только последний этаж»")
    public void shouldSeeLastFloorInUrl() {
        user.onProfSearchPage().extendFilters().clickDropdown("Последний этаж неважен");
        user.onProfSearchPage().selectPopup().item("Только последний этаж").click();
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lastFloor", "YES").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только не последний этаж»")
    public void shouldSeeNotLastFloorInUrl() {
        user.onProfSearchPage().extendFilters().clickDropdown("Последний этаж неважен");
        user.onProfSearchPage().selectPopup().item("Только не последний этаж").click();
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lastFloor", "NO").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Не первый этаж»")
    public void shouldSeeNotFirstFloorInUrl() {
        user.onProfSearchPage().extendFilters().selectCheckBox("Не первый этаж");
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorExceptFirst", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
