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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.QueryParams.DNO_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Feature(GEO_FEATURE)
@DisplayName("Установка гео куки при выборе региона")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SetGeoCookieTest {

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
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Москва").click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_REGION_ID, MOSCOW_ID_VALUE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена гео куки при выборе региона")
    public void shouldChangeGeoCookie() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, DNO_ID_VALUE);
        urlSteps.testing().open();
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Москва").click();

        basePageSteps.shouldSeeCookie(CLASSIFIED_REGION_ID, MOSCOW_ID_VALUE);
    }

}
