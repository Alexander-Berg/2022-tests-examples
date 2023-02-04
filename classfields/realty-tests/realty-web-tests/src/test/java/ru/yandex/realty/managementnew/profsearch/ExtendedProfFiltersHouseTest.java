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
import ru.auto.tests.commons.util.Utils;
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

@DisplayName("Профпоиск. Фильтры поиска для покупки дома.")
@Feature(PROFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class ExtendedProfFiltersHouseTest {

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
    public void before() {
        passportSteps.login(account);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID)
                .queryParam("category", "HOUSE").open();
        user.onProfSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом от»")
    public void shouldSeeHouseAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Площадь дома").input("от").sendKeys(areaFrom);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Дом до»")
    public void shouldSeeHouseAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Площадь дома").input("до").sendKeys(areaTo);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byExactName("Площадь участка").input("от").sendKeys(areaFrom);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lotAreaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byExactName("Площадь участка").input("до").sendKeys(areaTo);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("lotAreaMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }
}
