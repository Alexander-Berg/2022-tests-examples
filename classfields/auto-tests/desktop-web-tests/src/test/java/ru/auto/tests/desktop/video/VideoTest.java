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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@Story("Страница видео")
@DisplayName("Видео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VideoTest {

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
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение страницы")
    public void shouldSeeVideoPage() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/VideoSearchCarsPageSize8",
                "desktop/PostsVideos",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();

        basePageSteps.onVideoPage().h1().should(hasText("Тест-драйвы и обзоры автомобилей"));
        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(7)).forEach(item ->
                item.should(isDisplayed()));
        basePageSteps.onVideoPage().youtubeBlock().title().should(hasText("Со всего интернета"));
        basePageSteps.onVideoPage().youtubeBlock().itemsList().should(hasSize(8)).forEach(item ->
                item.should(isDisplayed()));
        basePageSteps.onVideoPage().articlesBlock().title().should(hasText("По теме в Журнале"));
        basePageSteps.onVideoPage().articlesBlock().itemsList().should(hasSize(4)).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по видео в блоке «Тест-драйвы и обзоры автомобилей»")
    @Category({Regression.class, Testing.class})
    public void shouldClickMagVideo() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsVideos").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().magVideoBlock().getItem(0).waitUntil(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path("elektrokary-tesla-nauchilis-govorit-" +
                        "pugayushchim-golosom-posmotrite-i-poslushayte-kak-eto-rabotaet/")
                .addParam("utm_content", "elektrokary-tesla-nauchilis-govorit-pugayushchim-golosom-" +
                        "posmotrite-i-poslushayte-kak-eto-rabotaet")
                .addParam("utm_campaign", "videopage_video")
                .addParam("utm_source", "auto-ru")
                .addParam("utm_medium", "cpm").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Показать ещё»")
    @Category({Regression.class, Testing.class})
    public void shouldClickShowMoreButton() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsVideos",
                "desktop/PostsVideosPage1").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().magVideoBlock().button("Показать ещё").click();

        basePageSteps.onVideoPage().magVideoBlock().itemsList().should(hasSize(13)).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по видео в блоке «Со всего интернета»")
    @Category({Regression.class, Testing.class})
    public void shouldClickYoutubeVideo() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/VideoSearchCarsPageSize8").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().youtubeBlock().getItem(0).waitUntil(isDisplayed()).click();

        basePageSteps.onVideoPage().activeVideo().should(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по статье в блоке «По теме в Журнале»")
    @Category({Regression.class, Testing.class})
    public void shouldClickArticle() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().articlesBlock().getItem(0).waitUntil(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path("shtorki-reylingi-farkop-neochevidnye-" +
                        "aksessuary-s-kotorymi-ne-proyti-tehosmotr/")
                .addParam("utm_content", "shtorki-reylingi-farkop-neochevidnye-aksessuary-s-kotorymi-" +
                        "ne-proyti-tehosmotr")
                .addParam("utm_campaign", "videopage_topics")
                .addParam("utm_source", "auto-ru")
                .addParam("utm_medium", "cpm").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по кнопке «Больше материалов»")
    @Category({Regression.class, Testing.class})
    public void shouldClickMoreArticlesButton() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/PostsArticles").post();

        urlSteps.testing().path(VIDEO).open();
        basePageSteps.onVideoPage().articlesBlock().button("Больше материалов").click();

        urlSteps.subdomain(SUBDOMAIN_MAG).addParam("utm_campaign", "videopage_topics")
                .addParam("utm_source", "auto-ru")
                .addParam("utm_medium", "cpm").shouldNotSeeDiff();
    }
}