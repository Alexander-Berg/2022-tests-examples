package ru.yandex.general.ads;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.BaseConstants.ClientType.TOUCH;
import static ru.yandex.general.consts.BaseConstants.ListingType.GRID;
import static ru.yandex.general.consts.GeneralFeatures.ADS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.ROSSIYA;

@Epic(LISTING_FEATURE)
@Feature(ADS_FEATURE)
@DisplayName("Ротация рекламных баннеров на главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class HomePageAdsBannerRotationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        goalsSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ротация рекламных баннеров на главной")
    public void shouldSeeBannerRotationHomepage() {
        basePageSteps.slowScrolling(15000);

        goalsSteps.shouldSeeAdBannerRotation(TOUCH, GRID);
    }

}
