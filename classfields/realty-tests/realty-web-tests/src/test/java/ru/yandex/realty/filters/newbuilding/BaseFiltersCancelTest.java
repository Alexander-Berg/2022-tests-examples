package ru.yandex.realty.filters.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Базовые фильтры поиска по новостройкам. Сброс параметров")
@Feature(FILTERS)
@Link("https://st.yandex-team.ru/VERTISTEST-1890")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersCancelTest {

    private static final String SITE_ID_VALUE = "375274";
    private static final String SITE_NAME_VALUE = "Саларьево парк";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(UrlSteps.SITE_ID, SITE_ID_VALUE).queryParam(UrlSteps.SITE_NAME, SITE_NAME_VALUE).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сбрасываем кнопкой «Сбросить всё»")
    public void shouldSeeUrlByCancelAll() {
        basePageSteps.onNewBuildingPage().filters().cancelAll().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).ignoreParam("showOutdated");
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сбрасываем бейджик")
    public void shouldSeeUrlByCancelBadge() {
        basePageSteps.onNewBuildingPage().filters().badge().click();
        basePageSteps.onNewBuildingPage().filters().badges(SITE_NAME_VALUE).clearGeo().click();
        basePageSteps.onNewBuildingPage().filters().badge().should(not(exists()));
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).shouldNotDiffWithWebDriverUrl();
    }
}
