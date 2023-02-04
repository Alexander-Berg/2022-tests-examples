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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.BANNERS;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Epic(MAIN)
@Feature(BANNERS)
@DisplayName("Смарт-баннер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SmartBannerTest {

    private static final String SMART_BANNER_CLOSED = "smart_banner_closed";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openMainPage() {
        basePageSteps.clearCookie(SMART_BANNER_CLOSED);
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в смарт-баннере")
    public void shouldSeeSmartBannerURL() {
        basePageSteps.onMobileMainPage().smartBanner().link().should(hasHref(equalTo("https://bzfk.adj.st/moskva/" +
                "kupit/kvartira/karta/?adjust_t=wsudr1e_e8tc29m&adjust_campaign=touch&adjust_adgroup=head_banner" +
                "&adjust_creative=basic_install_button&adjust_fallback=https%3A%2F%2Frealty.test.vertis.yandex.ru" +
                "&adjust_deeplink=yandexrealty%3A%2F%2Frealty.yandex.ru%2Fmoskva%2Fkupit%2Fkvartira%2Fkarta%2F")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливается кука smart_banner_closed при закрытии смарт-баннера")
    public void shouldSeeCookieAfterCloseBanner() {
        basePageSteps.onMobileMainPage().smartBanner().closeCross().click();

        assertThat("Проверяем наличие куки", basePageSteps.getCookieBy(SMART_BANNER_CLOSED).getValue(), equalTo("1"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смарт-баннер не показывается при наличии куки smart_banner_closed")
    public void shouldNotSeeSmartBannerWithCookie() {
        basePageSteps.setCookie(SMART_BANNER_CLOSED, "1", ".yandex.ru");
        basePageSteps.refresh();

        basePageSteps.onMobileMainPage().smartBanner().should(not(exists()));
    }

}
