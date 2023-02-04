package ru.yandex.realty.managementnew.profsearch;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Arrays;
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
@DisplayName("Профпоиск. Базовые фильтры.")
@Feature(PROFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseProfFiltersSearchTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    @ProfsearchAccount
    private Account account;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String address;

    @Parameterized.Parameter(1)
    public String[][] queryValues;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Проспект Вернадского 96к1", new String[][]{{"unifiedAddress", "Россия, Москва, проспект Вернадского, 96к1с1"}}},
                {"Университет", new String[][]{{"metroGeoId", "20444"}}}
        });
    }

    @Before
    public void before() {
        passportSteps.login(account);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam(RGID, MOSCOW_RGID).open();
        user.onProfSearchPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Строка поиска")
    public void shouldSeeStreetSearchInUrl() {
        user.onProfSearchPage().filters().geoInput().sendKeys(address);
        user.onProfSearchPage().filters().suggest().get(0).waitUntil(isDisplayed()).click();
        user.onProfSearchPage().filters().submitButton().click();
        Arrays.stream(queryValues).forEach(q -> urlSteps.queryParam(q[0], q[1]));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }
}
