package ru.auto.tests.mag.desktop;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.NIKITABUGAEV;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.mobile.page.MagPage.TEST_ARTICLE;
import static ru.auto.tests.desktop.page.AdsPage.C2;
import static ru.auto.tests.desktop.page.MagPage.WITH_DRAFT_MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал - статья - реклама")
@Feature(MAG)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopTestsModule.class)
public class MagArticleAds {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String bannerNumber;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {C2}
        });
    }

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(TEST_ARTICLE)
                .addParam(WITH_DRAFT_MODEL, "true").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NIKITABUGAEV)
    @DisplayName("Клик по рекламе")
    public void shouldClickTitle() {
        basePageSteps.onMagPage().poll().hover();
        basePageSteps.scrollDown(450);
        basePageSteps.onAdsPage().ad(bannerNumber).should(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
