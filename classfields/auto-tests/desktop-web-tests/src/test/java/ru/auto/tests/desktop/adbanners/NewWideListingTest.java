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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_BENZIN;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.page.AdsPage.C1;
import static ru.auto.tests.desktop.page.AdsPage.C2;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.JOURNAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Баннеры на листинге «CARS / NEW»")
@Epic(BANNERS)
@Feature(LISTING)
@Story("Широкий экран")
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NewWideListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public int snippetSequence;

    @Parameterized.Parameter(1)
    public String bannerNumber;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {0, C1},
                {22, C2},
                {36, C3},
                {36, JOURNAL}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Баннеры. CARS/NEW")
    public void shouldOpenBannerCarsNew() {
        urlSteps.testing().path(CARS).path(NEW).open();
        basePageSteps.onListingPage().getSale(snippetSequence).hover();
        basePageSteps.scrollDown(280);
        basePageSteps.hideElement(basePageSteps.onListingPage().stickySaveSearchPanel());
        basePageSteps.onAdsPage().ad(bannerNumber).waitUntil(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Баннеры CARS/NEW -> Бензин")
    public void shouldOpenBannerMarkModelSedan() {
        urlSteps.testing().path(CARS).path(NEW).path(ENGINE_BENZIN).open();
        basePageSteps.onListingPage().getSale(snippetSequence).hover();
        basePageSteps.scrollDown(300);
        basePageSteps.hideElement(basePageSteps.onListingPage().stickySaveSearchPanel());
        basePageSteps.onAdsPage().ad(bannerNumber).should(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
