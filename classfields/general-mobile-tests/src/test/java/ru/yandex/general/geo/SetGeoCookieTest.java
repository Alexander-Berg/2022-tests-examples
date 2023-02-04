package ru.yandex.general.geo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.QueryParams.DNO_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.NIZHNIJ_NOVGOROD_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.SANKT_PETERBURG_ID_VALUE;
import static ru.yandex.general.mobile.element.FiltersPopup.REGION;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Feature(GEO_FEATURE)
@DisplayName("Установка гео куки при выборе региона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SetGeoCookieTest {

    private static final String SANKT_PETERBURG = "Санкт-Петербург";
    private static final String NIZHNIJ_NOVGOROD = "Нижний Новгород";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Установка гео куки при выборе региона")
    public void shouldSeeGeoCookie() {
        urlSteps.testing().open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.onListingPage().popup(REGION).input().sendKeys(NIZHNIJ_NOVGOROD);
        basePageSteps.onListingPage().popup(REGION).spanLink(NIZHNIJ_NOVGOROD).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        basePageSteps.assertCookieValueIs(CLASSIFIED_REGION_ID, NIZHNIJ_NOVGOROD_ID_VALUE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена гео куки при выборе региона")
    public void shouldChangeGeoCookie() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, DNO_ID_VALUE);
        urlSteps.testing().open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup(REGION).spanLink(SANKT_PETERBURG).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        basePageSteps.assertCookieValueIs(CLASSIFIED_REGION_ID, SANKT_PETERBURG_ID_VALUE);
    }

}

