package ru.auto.tests.desktop.adbanners;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.AdsPage.C1;
import static ru.auto.tests.desktop.page.AdsPage.C2;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг. Формирование таргетов «CARS / ALL»")
@Epic(BANNERS)
@Feature(LISTING)
@Story("Узкий экран")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopTestsModule.class)
public class ListingWideSpecialGeoTest {

    private static final String NOT_A_MOSCOW = "11162"; // регион из списка https://bunker.yandex-team.ru/auto_ru/common/ad_free_rids?v=7&view=raw

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public int snippetSequence;

    @Parameterized.Parameter(1)
    public String bannerNumber;

    @Parameterized.Parameter(2)
    public String type;

    @Parameterized.Parameters(name = "name = {index}: {1} {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {0, C1, USED},
                {0, R1, USED},
                {23, C2, USED},
                {0, C1, NEW},
                {23, C2, NEW}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        cookieSteps.setRegion(NOT_A_MOSCOW);
        urlSteps.testing().path(CARS).path(type).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Баннеры. Не Москва. Широкий экран")
    public void shouldOpenBannerSpecialRegionWide() {
        basePageSteps.onListingPage().getSale(snippetSequence).hover();
        basePageSteps.scrollDown(100);
        basePageSteps.hideElement(basePageSteps.onListingPage().stickySaveSearchPanel());
        basePageSteps.onAdsPage().ad(bannerNumber).waitUntil(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
