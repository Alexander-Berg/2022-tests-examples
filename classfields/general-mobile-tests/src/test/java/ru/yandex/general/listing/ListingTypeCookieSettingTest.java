package ru.yandex.general.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_TYPE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.mobile.step.BasePageSteps.LIST;

@Epic(LISTING_FEATURE)
@Feature(LISTING_TYPE)
@DisplayName("Установка куки с типом листинга")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingTypeCookieSettingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String cookieValue;

    @Parameterized.Parameters(name = "{index}. Тип листинга «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Плиткой", GRID},
                {"Списком", LIST}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.resize(375, 1500);
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Установка куки с типом листинга")
    public void shouldSeeListingTypeCookie() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(name).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, cookieValue);
    }

}
