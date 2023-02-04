package ru.yandex.realty.specproject.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Спецпроект. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AreaSpecProjectFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтр площади от")
    public void shouldSeeAreaFrom() {
        String area = String.valueOf(getNormalArea());
        basePageSteps.onSpecProjectPage().filters().areaFrom().sendKeys(area);
        basePageSteps.onSpecProjectPage().filters().submitButton().click();
        urlSteps.queryParam(UrlSteps.AREA_MIN_URL_PARAM, area).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтр площади до")
    public void shouldSeeAreaTo() {
        String area = String.valueOf(getNormalArea());
        basePageSteps.onSpecProjectPage().filters().areaTo().sendKeys(area);
        basePageSteps.onSpecProjectPage().filters().submitButton().click();
        urlSteps.queryParam(UrlSteps.AREA_MAX_URL_PARAM, area).shouldNotDiffWithWebDriverUrl();
    }
}
