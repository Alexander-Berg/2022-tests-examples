package ru.auto.tests.desktop.adbanners;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на главной")
@Epic(BANNERS)
@Feature(MAIN)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopTestsModule.class)
public class MainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public int scrollPx;

    @Parameterized.Parameter(1)
    public String bannerNumber;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
            //    {0, TOP}, TODO Понять, почему в тесте не появляется баннер
                {4000, C3}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().addParam(FROM, "autotest").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Баннер {bannerNumber} на главной")
    public void shouldOpenBannerMarkModelSedan() {
        basePageSteps.scrollDown(scrollPx);
        basePageSteps.onAdsPage().ad(bannerNumber).should(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
