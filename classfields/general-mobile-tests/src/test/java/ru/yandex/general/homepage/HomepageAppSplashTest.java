package ru.yandex.general.homepage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.APP_SPLASH;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.mobile.step.BasePageSteps.APP_SPLASH_SHOWN;
import static ru.yandex.general.mobile.step.BasePageSteps.SEEN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(APP_SPLASH)
@DisplayName(APP_SPLASH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class HomepageAppSplashTest {

    private static final String SPLASH_LINK = "https://redirect.appmetrica.yandex.com/serve/387432876397905378/?afpub_id=main&creative_id";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.clearCookie(APP_SPLASH_SHOWN);
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка сплэш баннера установки приложения на главной")
    public void shouldSeeAppSplashBannerLink() {
        basePageSteps.onListingPage().bottomSplashBanner().link().should(hasAttribute(HREF, containsString(SPLASH_LINK)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливается кука «APP_SPLASH_SHOWN = seen» при показе сплэш баннера на главной")
    public void shouldSeeAppSplashBannerCookieAfterShowBanner() {
        basePageSteps.onListingPage().bottomSplashBanner().waitUntil(isDisplayed());

        basePageSteps.shouldSeeCookie(APP_SPLASH_SHOWN, SEEN);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается сплэш баннер с кукой «APP_SPLASH_SHOWN = seen» на главной")
    public void shouldNotSeeAppSplashBannerWithCookie() {
        basePageSteps.onListingPage().bottomSplashBanner().waitUntil(isDisplayed());
        basePageSteps.refresh();

        basePageSteps.onListingPage().bottomSplashBanner().should(not(isDisplayed()));
    }

}
