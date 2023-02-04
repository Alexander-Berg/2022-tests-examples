package ru.auto.tests.mobile.video;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Видео")
@Feature(AutoruFeatures.VIDEO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VideoTest {

    private static final String MARK = "Kia";
    private static final String MODEL = "Optima";
    private static final String GENERATION = "IV Рестайлинг";
    private static final String GENERATION_CODE = "21342050";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение страницы")
    public void shouldSeeVideoPage() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/VideoSearchCarsPageSize8",
                "mobile/PostsVideos",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().h1().should(hasText("Тест-драйвы и обзоры автомобилей"));
        basePageSteps.onVideoPage().journalVideosList().should(hasSize(4)).forEach(item ->
                item.should(isDisplayed()));
        basePageSteps.onVideoPage().youtubeVideoBlockTitle().should(hasText("Со всего интернета"));
        basePageSteps.onVideoPage().youtubeVideosList().should(hasSize(8)).forEach(item ->
                item.should(isDisplayed()));
        basePageSteps.onVideoPage().journalArticlesBlockTitle().should(hasText("По теме в Журнале"));
        basePageSteps.onVideoPage().journalArticlesList().should(hasSize(4)).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по видео в блоке «Со всего интернета»")
    @Category({Regression.class, Testing.class})
    public void shouldClickYoutubeVideo() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/VideoSearchCarsPageSize8").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().getYoutubeVideo(0).click();
        basePageSteps.onVideoPage().videoFrame().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Клик по видео в блоке «Тест-драйвы и обзоры автомобилей»")
    @Category({Regression.class, Testing.class})
    public void shouldClickJournalVideo() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/PostsVideos").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().getJournalVideo(0).waitUntil(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path("elektrokary-tesla-nauchilis-govorit-" +
                "pugayushchim-golosom-posmotrite-i-poslushayte-kak-eto-rabotaet/")
                .addParam("utm_content", "elektrokary-tesla-nauchilis-govorit-pugayushchim-golosom-" +
                        "posmotrite-i-poslushayte-kak-eto-rabotaet")
                .addParam("utm_campaign","videopage_video")
                .addParam("utm_source","auto-ru")
                .addParam("utm_medium","cpm").shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Клик по статье в блоке «По теме в журнале»")
    @Category({Regression.class, Testing.class})
    public void shouldClickJournalArticle() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().getJournalArticle(0).waitUntil(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path("shtorki-reylingi-farkop-neochevidnye-" +
                "aksessuary-s-kotorymi-ne-proyti-tehosmotr/")
                .addParam("utm_content", "shtorki-reylingi-farkop-neochevidnye-aksessuary-s-kotorymi-" +
                        "ne-proyti-tehosmotr")
                .addParam("utm_campaign","videopage_topics")
                .addParam("utm_source","auto-ru")
                .addParam("utm_medium","cpm").shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Клик по кнопке «Показать ещё»")
    @Category({Regression.class, Testing.class})
    public void shouldClickShowMoreButton() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/PostsVideos",
                "mobile/PostsVideosPage1").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().journalVideosList().should(hasSize(4)).forEach(item ->
                item.should(isDisplayed()));
        basePageSteps.onVideoPage().button("Показать ещё").click();
        basePageSteps.onVideoPage().journalVideosList().should(hasSize(8)).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Клик по кнопке «Больше материалов»")
    @Category({Regression.class, Testing.class})
    public void shouldClickMoreArticlesButton() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().button("Больше материалов").click();
        urlSteps.subdomain(SUBDOMAIN_MAG).addParam("utm_campaign","videopage_topics")
                .addParam("utm_source","auto-ru")
                .addParam("utm_medium","cpm").shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Фильтр по марке")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByMark() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/VideoSearchCarsKiaPageSize8",
                "desktop/PostsArticlesKia",
                "mobile/PostsVideosKia").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().filters().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.scrollAndClick(basePageSteps.onVideoPage().mmmPopup().popularMark(MARK));
        basePageSteps.onVideoPage().mmmPopup().applyFiltersButton().click();
        urlSteps.path(CARS).path(MARK.toLowerCase()).path("/").shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1().waitUntil(hasText(format("Тест-драйвы %s", MARK)));
        basePageSteps.onVideoPage().filters().should(hasText(format("%s\nВсе модели / Все поколения", MARK)));
        basePageSteps.onVideoPage().journalVideosList().waitUntil(hasSize(4));
        basePageSteps.onVideoPage().youtubeVideosList().waitUntil(hasSize(8));
        basePageSteps.onVideoPage().journalArticlesList().waitUntil(hasSize(4));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Фильтр по модели")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByModel() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/SearchCarsBreadcrumbsKiaOptima",
                "desktop/VideoSearchCarsKiaOptimaPageSize8",
                "desktop/PostsArticlesKiaOptima",
                "mobile/PostsVideosKiaOptima").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().filters().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.scrollAndClick(basePageSteps.onVideoPage().mmmPopup().popularMark(MARK));
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.scrollAndClick(basePageSteps.onVideoPage().mmmPopup().popularModel(MODEL));
        urlSteps.path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path("/").shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1().waitUntil(hasText(format("Тест-драйвы %s %s", MARK, MODEL)));
        basePageSteps.onVideoPage().filters().waitUntil(hasText(format("%s\n%s\nПоколение", MARK, MODEL)));
        basePageSteps.onVideoPage().journalVideosList().waitUntil(hasSize(4));
        basePageSteps.onVideoPage().youtubeVideosList().waitUntil(hasSize(8));
        basePageSteps.onVideoPage().journalArticlesList().waitUntil(hasSize(4));
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @DisplayName("Фильтр по поколению")
    @Category({Regression.class, Testing.class})
    public void shouldSeeVideosByGeneration() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsKia",
                "desktop/SearchCarsBreadcrumbsKiaOptima",
                "desktop/SearchCarsBreadcrumbsKiaOptima21342050",
                "desktop/VideoSearchCarsKiaOptimaPageSize8",
                "desktop/VideoSearchCarsKiaOptima21342050PageSize8",
                "desktop/PostsArticlesKiaOptima",
                "mobile/PostsVideosKiaOptima").post();

        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).open();
        basePageSteps.onVideoPage().filters().button("Поколение").click();
        basePageSteps.onVideoPage().mmmPopup().generation(GENERATION).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_CODE).path("/").shouldNotSeeDiff();
        basePageSteps.onVideoPage().h1()
                .waitUntil(hasText(format("Тест-драйвы %s %s %s", MARK, MODEL, GENERATION)));
        basePageSteps.onVideoPage().filters().waitUntil(hasText(format("%s\n%s (%s)", MARK, MODEL, GENERATION)));
        basePageSteps.onVideoPage().journalVideosList().waitUntil(hasSize(4));
        basePageSteps.onVideoPage().youtubeVideosList().waitUntil(hasSize(8));
        basePageSteps.onVideoPage().journalArticlesList().waitUntil(hasSize(4));
    }
}
