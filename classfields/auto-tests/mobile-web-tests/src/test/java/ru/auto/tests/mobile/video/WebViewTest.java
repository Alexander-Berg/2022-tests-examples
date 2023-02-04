package ru.auto.tests.mobile.video;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.auto.tests.desktop.consts.QueryParams.ONLY_CONTENT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Видео в webview")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class WebViewTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(VIDEO).addParam(ONLY_CONTENT, "true").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение страницы видео в WebView")
    @Category({Regression.class})
    public void shouldSeeVideoInWebView() {
        basePageSteps.onVideoPage().filters().should(isDisplayed());
        basePageSteps.onVideoPage().youtubeVideosList().should(hasSize(greaterThan(0)));
        basePageSteps.onVideoPage().journalVideosList().should(hasSize(greaterThan(0)));
        basePageSteps.onVideoPage().journalArticlesList().should(hasSize(greaterThan(0)));
    }
}
