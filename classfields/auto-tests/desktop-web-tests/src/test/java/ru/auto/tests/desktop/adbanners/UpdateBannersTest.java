package ru.auto.tests.desktop.adbanners;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.AdsPage.C1;
import static ru.auto.tests.desktop.page.AdsPage.C2;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.JOURNAL;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на листинге «CARS / ALL»")
@Epic(BANNERS)
@Feature(LISTING)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class UpdateBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(CARS).path(USED).open();
    }

    @Test
    @Issue("AUTORUFRONT-21811")
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Повторная загрузка всех баннеров при изменении фильтров")
    public void shouldUpdateBannersCarsAll() {
        basePageSteps.onListingPage().filter().selectItem("Марка", "Hyundai");
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        waitSomething(5, TimeUnit.SECONDS);

        basePageSteps.onAdsPage().ad(C1).should(isDisplayed());
        basePageSteps.onAdsPage().ad(R1).should(isDisplayed());

        basePageSteps.onListingPage().getSale(22).hover();
        basePageSteps.scrollDown(100);

        basePageSteps.onAdsPage().ad(C2).should(isDisplayed());

        basePageSteps.onListingPage().getSale(36).hover();
        basePageSteps.scrollDown(300);

        basePageSteps.onAdsPage().ad(C3).should(isDisplayed());
        basePageSteps.onAdsPage().ad(JOURNAL).should(isDisplayed());
    }

}
