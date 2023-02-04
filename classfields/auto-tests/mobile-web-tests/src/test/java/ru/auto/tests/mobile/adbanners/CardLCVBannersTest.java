package ru.auto.tests.mobile.adbanners;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.TOP_MOBILE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры в карточке LCV")
@Feature(BANNERS)
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardLCVBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String banner;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {TOP_MOBILE},
                {C3}
        });
    }

    @Before
    public void before() {
        cookieSteps.setCookie("card_prevnext_swipe_info", "1",
                format(".%s", urlSteps.getConfig().getBaseDomain()));
        urlSteps.testing().path(LCV).path(USED).open();
        basePageSteps.onListingPage().getSale(3).title().click();
        basePageSteps.onCardPage().features().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру на карточке")
    public void shouldClickBannerInCard() {
        basePageSteps.onAdsPage().shouldSeeAds(greaterThan(0));
        basePageSteps.scrollAndClick(basePageSteps.onAdsPage().ad(banner));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
