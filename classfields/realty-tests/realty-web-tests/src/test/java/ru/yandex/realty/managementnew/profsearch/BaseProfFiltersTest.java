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

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.PROFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.page.ProfSearchPage.MOSCOW_RGID;
import static ru.yandex.realty.page.ProfSearchPage.RGID;

/**
 * @author kantemirov
 */
@DisplayName("Профпоиск. Базовые фильтры.")
@Feature(PROFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class BaseProfFiltersTest {

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
        user.onProfSearchPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «цена от»")
    public void shouldSeePriceMinInUrl() {
        String priceMin = valueOf(getRandomShortInt());
        user.onProfSearchPage().filters().price().input(PRICE_FROM).sendKeys(priceMin);
        user.onProfSearchPage().filters().submitButton().click();
        urlSteps.queryParam("priceMin", priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «цена до»")
    public void shouldSeePriceMaxInUrl() {
        String priceMax = valueOf(getRandomShortInt());
        user.onProfSearchPage().filters().price().input(TO).sendKeys(priceMax);
        user.onProfSearchPage().filters().submitButton().click();
        urlSteps.queryParam("priceMax", priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «за м²»")
    public void shouldSeePriceTypeInUrl() {
        user.onProfSearchPage().filters().select("за всё", "за м²");
        user.onProfSearchPage().filters().submitButton().click();
        urlSteps.queryParam("priceType", "PER_METER").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «за сотку»")
    public void shouldSeePricePerAreInUrl() {
        user.onProfSearchPage().filters().dropDownButton("Купить квартиру").click();
        user.onProfSearchPage().filters().selectPopup().item("Участок").click();
        user.onProfSearchPage().filters().select("за всё", "за сотку");
        user.onProfSearchPage().filters().submitButton().click();
        urlSteps.queryParam("category", "LOT").queryParam("priceType", "PER_ARE").shouldNotDiffWithWebDriverUrl();
    }
}
