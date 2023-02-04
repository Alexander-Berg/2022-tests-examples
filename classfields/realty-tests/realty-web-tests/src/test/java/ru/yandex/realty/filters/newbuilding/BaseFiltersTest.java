package ru.yandex.realty.filters.newbuilding;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

/**
 * Created by vicdev on 17.04.17.
 */

@DisplayName("Базовые фильтры поиска по новостройкам")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openNewBuildingPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        user.onNewBuildingPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(VICDEV)
    @DisplayName("Все параметры «количество комнат»")
    public void shouldSeeAllRoomTotalParamsInUrl() {
        user.onNewBuildingPage().filters().selectCheckBox("Студия");
        user.onNewBuildingPage().filters().selectCheckBox("1");
        user.onNewBuildingPage().filters().selectCheckBox("2");
        user.onNewBuildingPage().filters().selectCheckBox("3");
        user.onNewBuildingPage().filters().selectCheckBox("4+");

        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.queryParam("roomsTotal", "1", "2", "3", "PLUS_4", "STUDIO").shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(VICDEV)
    @DisplayName("Параметр цена «от»")
    public void shouldSeePriceMinInUrl() {
        String priceMin = valueOf(getRandomShortInt());
        user.onNewBuildingPage().filters().price().input("от").sendKeys(priceMin);
        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.queryParam("priceMin", priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(VICDEV)
    @DisplayName("Параметр цена «до»")
    public void shouldSeePriceMaxInUrl() {
        String priceMax = valueOf(getRandomShortInt());
        user.onNewBuildingPage().filters().price().input("до").sendKeys(priceMax);
        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.queryParam("priceMax", priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(VICDEV)
    @DisplayName("Параметр «за все/за квадратный метр»")
    public void shouldSeePriceTypeInUrl() {
        user.onNewBuildingPage().filters().selectButton("м²");
        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.queryParam("priceType", "PER_METER").shouldNotDiffWithWebDriverUrl();
    }
}
