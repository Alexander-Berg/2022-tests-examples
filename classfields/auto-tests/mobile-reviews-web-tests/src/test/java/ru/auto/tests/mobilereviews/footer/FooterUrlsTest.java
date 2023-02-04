package ru.auto.tests.mobilereviews.footer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер - ссылки")
@Feature(FOOTER)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String urlTitle;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Дилерам", "https://%s/dealer/"},
                {"Помощь", YANDEX_SUPPORT_AUTORU},
                {"Саша Котов", "https://%s/kot/"},
                {"О проекте", "https://%s/home/"},
                {"Аналитика Авто.ру", "https://mag.%s/tag/research/?from=morda-stat"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SessionUnauth").post();

        urlSteps.testing().path(REVIEWS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onReviewsMainPage().footer().button(urlTitle).waitUntil(isDisplayed()).hover().click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
