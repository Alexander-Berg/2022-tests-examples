package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.BANNERS;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.mobile.element.PageBanner.THANKS_NEXT_TIME;

@Issue("VERTISTEST-1378")
@Epic(MAIN)
@Feature(BANNERS)
@DisplayName("Сплэш-баннер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SplashBannerTest {

    private static final String SPLASH_BANNER_CLOSED = "splash_banner_closed";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.clearCookie(SPLASH_BANNER_CLOSED);
        basePageSteps.refresh();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливается кука splash_banner_closed при открытии сплэш-баннера")
    public void shouldSeeCookieAfterOpenBanner() {
        assertThat("Проверяем наличие куки", basePageSteps.getCookieBy(SPLASH_BANNER_CLOSED).getValue(), equalTo("1"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сплэш-баннер не показывается при наличии куки splash_banner_closed")
    public void shouldNotSeeSplashBannerWithCookie() {
        basePageSteps.setCookie(SPLASH_BANNER_CLOSED, "1", ".yandex.ru");
        basePageSteps.refresh();

        basePageSteps.onMobileMainPage().splashBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Остаемся на главной после закрытия сплэш-баннера")
    public void shouldSeeMainPageAfterCloseSplashBanner() {
        basePageSteps.onMobileMainPage().splashBanner().close().click();

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Остаемся на главной после нажатия на «Спасибо, в другой раз»")
    public void shouldSeeMainPageAfterSplashBannerThanksClick() {
        basePageSteps.onMobileMainPage().splashBanner().spanLink(THANKS_NEXT_TIME).click();

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }
}
