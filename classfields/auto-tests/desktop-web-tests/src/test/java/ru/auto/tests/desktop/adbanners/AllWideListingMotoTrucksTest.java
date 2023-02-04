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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.AdsPage.C1;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.JOURNAL;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на листинге «MOTO / ALL»  и «TRUCKS / ALL»")
@Epic(BANNERS)
@Feature(LISTING)
@Story("Широкий экран")
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AllWideListingMotoTrucksTest {

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
    public int snippetSequence;

    @Parameterized.Parameter(2)
    public String bannerNumber;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {MOTORCYCLE, 0, R1},
                {MOTORCYCLE, 0, C1},
                {ATV, 36, C3},
                {ATV, 36, JOURNAL},

                {LCV, 0, R1},
                {LCV, 0, C1},
                {TRUCKS, 36, C3},
                {TRUCKS, 36, JOURNAL}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(category).path(ALL).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Баннеры. Листинг мото и коммерческих ТС")
    public void shouldOpenBannerMotoTrucksAll() {
        basePageSteps.onListingPage().getSale(snippetSequence).hover();
        basePageSteps.scrollDown(280);
        basePageSteps.hideElement(basePageSteps.onListingPage().stickySaveSearchPanel());
        basePageSteps.onAdsPage().ad(bannerNumber).waitUntil(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
