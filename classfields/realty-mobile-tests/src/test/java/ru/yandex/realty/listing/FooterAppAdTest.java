package ru.yandex.realty.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Ссылка на приложение в футере")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FooterAppAdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на приложение в футере")
    public void shouldSeeAppUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().footer().appAd().link().should(hasHref(equalTo(
                "https://redirect.appmetrica.yandex.com/serve/529424651634368014?click_id={transaction_id}" +
                        "&google_aid={GAID}&creative_id=blue_app")));
    }

}
