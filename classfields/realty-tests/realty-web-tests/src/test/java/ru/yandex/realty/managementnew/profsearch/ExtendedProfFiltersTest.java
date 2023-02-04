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

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.PROFILTERS;
import static ru.yandex.realty.page.ProfSearchPage.MOSCOW_RGID;
import static ru.yandex.realty.page.ProfSearchPage.RGID;


@DisplayName("Профпоиск. Расширенные фильтры поиска по объявлениям, относящиеся к характеристикам дома.")
@Feature(PROFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class ExtendedProfFiltersTest {

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
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID).open();
        user.onProfSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Общая площадь от»")
    public void shouldSeeAreaMinInUrl() {
        String spaceMin = valueOf(getRandomShortInt());
        user.onProfSearchPage().extendFilters().byExactName("Площадь").input("от").sendKeys(spaceMin);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMin", spaceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Общая площадь до»")
    public void shouldSeeAreaMaxInUrl() {
        String spaceMax = valueOf(getRandomShortInt());
        user.onProfSearchPage().extendFilters().byExactName("Площадь").input("до").sendKeys(spaceMax);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("areaMax", spaceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Кухня до»")
    public void shouldSeeKitchenAreaMaxInUrl() {
        String spaceMax = valueOf(getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Площадь кухни от").input().sendKeys(spaceMax);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("kitchenSpaceMin", spaceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж c»")
    public void shouldSeeFloorMinInUrl() {
        String spaceMin = valueOf(getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Этаж").input("c").sendKeys(spaceMin);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMin", spaceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж по»")
    public void shouldSeeFloorMaxInUrl() {
        String spaceMax = valueOf(getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Этаж").input("по").sendKeys(spaceMax);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("floorMax", spaceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этажей в доме»")
    public void shouldNoLessThanFloorsInUrl() {
        String floors = String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Этажей в доме").input().sendKeys(floors);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("minFloors", floors).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Год от»")
    public void shouldSeeYearMinInUrl() {
        String yearFrom = "197" + String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Год постройки").input("c").sendKeys(yearFrom);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("builtYearMin", yearFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Год до»")
    public void shouldSeeYearMaxInUrl() {
        String yearTo = "197" + String.valueOf(Utils.getRandomShortInt());
        user.onProfSearchPage().extendFilters().byName("Год постройки").input("по").sendKeys(yearTo);
        user.onProfSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("builtYearMax", yearTo).shouldNotDiffWithWebDriverUrl();
    }
}
