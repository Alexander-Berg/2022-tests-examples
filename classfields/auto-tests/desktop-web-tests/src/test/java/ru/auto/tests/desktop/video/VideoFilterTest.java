package ru.auto.tests.desktop.video;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Locale;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.auto.tests.desktop.page.VideoPage.H1_TEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AutoruFeatures.VIDEO)
@Story("Фильтр")
@DisplayName("Видео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VideoFilterTest {

    private static final String MARK = "Kia";
    private static final String MODEL = "Optima";
    private static final String MODEL_2 = "Ceed";
    private static final String MODEL_NAMEPLATE = "Proceed";
    private static final String MODEL_NAMEPLATE_URL = "/ceed-proceed/";
    private static final String GENERATION = "IV Рестайлинг";
    private static final String GENERATION_CODE = "21342050";

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

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр по марке")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByMark() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/VideoSearchCarsKiaPageSize8",
                "desktop/PostsArticlesKia",
                "desktop/PostsVideosKia").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().mmmFilter().selectMark(MARK);

        urlSteps.path(CARS).path(MARK.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1().waitUntil(hasText(format("%s %s", H1_TEXT, MARK)));
        basePageSteps.onVideoPage().mmmFilter().markSelect().should(hasText(MARK));
        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(7));
        basePageSteps.onVideoPage().youtubeBlock().itemsList().should(hasSize(8));
        basePageSteps.onVideoPage().articlesBlock().itemsList().should(hasSize(4));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр по модели")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByModel() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/SearchCarsBreadcrumbsKiaOptima",
                "desktop/VideoSearchCarsKiaOptimaPageSize8",
                "desktop/PostsArticlesKiaOptima",
                "desktop/PostsVideosKiaOptima").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().mmmFilter().selectMark(MARK);
        basePageSteps.onVideoPage().mmmFilter().selectModel(MODEL);

        urlSteps.path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1().should(hasText(format("%s %s %s", H1_TEXT, MARK, MODEL)));
        basePageSteps.onVideoPage().mmmFilter().markSelect().should(hasText(MARK));
        basePageSteps.onVideoPage().mmmFilter().modelSelect().should(hasText(MODEL));
        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(7));
        basePageSteps.onVideoPage().youtubeBlock().itemsList().should(hasSize(8));
        basePageSteps.onVideoPage().articlesBlock().itemsList().should(hasSize(4));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр по поколению")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByGeneration() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/SearchCarsBreadcrumbsKiaOptima",
                "desktop/SearchCarsBreadcrumbsKiaOptima21342050",
                "desktop/VideoSearchCarsKiaOptima21342050PageSize8",
                "desktop/PostsArticlesKiaOptima",
                "desktop/PostsVideosKiaOptima").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().mmmFilter().selectMark(MARK);
        basePageSteps.onVideoPage().mmmFilter().selectModel(MODEL);
        basePageSteps.onVideoPage().mmmFilter().selectGenerationInPopup(GENERATION);

        urlSteps.path(CARS).path(MARK.toLowerCase(Locale.ROOT)).path(MODEL.toLowerCase())
                .path(GENERATION_CODE).path(SLASH).shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1()
                .waitUntil(hasText(format("%s %s %s %s", H1_TEXT, MARK, MODEL, GENERATION)));
        basePageSteps.onVideoPage().mmmFilter().markSelect().should(hasText(MARK));
        basePageSteps.onVideoPage().mmmFilter().modelSelect().should(hasText(MODEL));
        basePageSteps.onVideoPage().mmmFilter().generationSelect().should(hasText(format("2018—2020 %s", GENERATION)));
        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(7));
        basePageSteps.onVideoPage().youtubeBlock().itemsList().should(hasSize(8));
        basePageSteps.onVideoPage().articlesBlock().itemsList().should(hasSize(4));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Фильтр по шильду")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByNameplate() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/SearchCarsBreadcrumbsKiaCeed",
                "desktop/VideoSearchCarsKiaCeedPageSize8",
                "desktop/PostsArticlesKiaCeed",
                "desktop/PostsVideosKiaCeed").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().mmmFilter().selectMark(MARK);
        basePageSteps.onVideoPage().mmmFilter().selectNameplate(MODEL_NAMEPLATE, MODEL_2);

        urlSteps.path(CARS).path(MARK.toLowerCase()).path(MODEL_NAMEPLATE_URL).shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1().waitUntil(hasText(format("%s %s %s %s", H1_TEXT, MARK, MODEL_2, MODEL_NAMEPLATE)));
        basePageSteps.onVideoPage().mmmFilter().markSelect().should(hasText(MARK));
        basePageSteps.onVideoPage().mmmFilter().modelSelect().should(hasText(MODEL_NAMEPLATE));
        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(2));
        basePageSteps.onVideoPage().youtubeBlock().itemsList().should(hasSize(2));
        basePageSteps.onVideoPage().articlesBlock().itemsList().should(hasSize(2));
    }
}