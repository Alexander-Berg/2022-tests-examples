package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в футере. Станции")
@Feature(MAIN)
@Link("https://st.yandex-team.ru/VERTISTEST-2140")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FooterLinksRailwaysTest {

    private static final String STATION_LINK = "Станции пригородных поездов";
    private static final String HREF_ATTRIBUTE = "href";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeStationCityLink() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().footer().link(STATION_LINK)
                .should(hasAttribute(HREF_ATTRIBUTE, containsString("/moskva/railways/")));
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeStationPreCityLink() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.onBasePage().footer().link(STATION_LINK)
                .should(hasAttribute(HREF_ATTRIBUTE, containsString("/moskva_i_moskovskaya_oblast/railways/")));
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldNotSeeStationCityLink() {
        urlSteps.testing().path("/yakutsk/").open();
        basePageSteps.onBasePage().footer().link(STATION_LINK).should(not(isDisplayed()));
    }
}
