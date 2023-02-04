package ru.yandex.general.banners;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import static ru.yandex.general.consts.GeneralFeatures.BANNER;
import static ru.yandex.general.consts.Owners.ALEXANDERREX;
import static ru.yandex.general.mobile.page.ListingPage.AUTO;
import static ru.yandex.general.mobile.page.ListingPage.REALTY;
import static ru.yandex.general.mobile.page.ListingPage.WORK;

@Epic(BANNER)
@DisplayName("Переход по баннерам Авто, Недвижимость, Работа с главной страницы")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BannersExternalLinksTest {

    private static final String AUTO_BANNER_LINK = "https://m.auto.ru/cars/all/?from=classified&geo_id=213&utm_content=banner_main&utm_source=yandex_ads";
    private static final String REALTY_BANNER_LINK = "https://m.realty.yandex.ru/moskva/kupit/kvartira/?from=classified&gid=213&utm_content=banner_main&utm_source=yandex_ads&redirect_from_rgid=true";
    private static final String RABOTA_BANNER_LINK = "https://o.yandex.ru/moskva/rabota/?from=classified&utm_content=banner_main&utm_source=yandex_ads";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String bannerName;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "{index}. Баннер {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {AUTO, AUTO_BANNER_LINK},
                {REALTY, REALTY_BANNER_LINK},
                {WORK, RABOTA_BANNER_LINK},
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @DisplayName("Переход по баннерам Авто/Недвижимости/Работы с главной")
    public void shouldSeeGoToBannerFromMain() {
        basePageSteps.onListingPage().banner(bannerName).click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(url).shouldNotDiffWithWebDriverUrl();
    }

}
