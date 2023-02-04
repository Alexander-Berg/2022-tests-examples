package ru.auto.tests.mobile.adbanners;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.page.AdsPage.TOP_MOBILE;

@DisplayName("ТОП баннер в листингах cars, moto, trucks")
@Feature(BANNERS)
@GuiceModules(MobileTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingTopBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {CARS, "hyundai/solaris"},
                {MOTORCYCLE, "honda/cbr_1100"},
                {LCV, "mercedes/sprinter"}
        });
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру топ")
    public void shouldClickBannerTop() {
        urlSteps.testing().path(category).path(url).path(ALL).open();
        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.scrollDown(50);
        basePageSteps.scrollDown(100);
        basePageSteps.onAdsPage().ad(TOP_MOBILE).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
