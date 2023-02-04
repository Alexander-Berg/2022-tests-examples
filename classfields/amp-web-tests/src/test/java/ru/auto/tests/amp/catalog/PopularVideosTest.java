package ru.auto.tests.amp.catalog;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Каталог - популярные видео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PopularVideosTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a5";
    private static final String GENERATION_ID = "20795592";
    private static final String BODY_ID = "20795627";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Видео в блоке «Популярные видео»")
    public void shouldSeePopularVideos() {
        basePageSteps.onCatalogBodyPage().popularVideos().videosList().should(hasSize(greaterThan(0)))
                .forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все видео»")
    @Category({Regression.class})
    public void shouldClickAllVideosUrl() {
        basePageSteps.onCatalogBodyPage().popularVideos().allVideosUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path("/")
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео")
    @Category({Regression.class})
    public void shouldClickVideo() {
        basePageSteps.onCatalogBodyPage().popularVideos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogBodyPage().popularVideos().popup().waitUntil(isDisplayed());
    }
}
