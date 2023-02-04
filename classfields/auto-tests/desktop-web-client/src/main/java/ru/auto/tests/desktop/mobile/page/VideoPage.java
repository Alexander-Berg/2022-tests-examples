package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.element.Filters;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VideoPage extends BasePage, WithMmmPopup {

    @Name("Фильтры")
    @FindBy("//div[@class = 'MiniFilter']")
    Filters filters();

    @Name("Список видео Youtube")
    @FindBy("//div[contains(@class, 'VideoList__item')] | " +
            "//div[contains(@class, 'PageVideoMobileDumb__youtubeItem')]")
    ElementsCollection<VertisElement> youtubeVideosList();

    @Name("Фрейм видео")
    @FindBy("//iframe[@id = 'videoFrame']")
    VertisElement videoFrame();

    @Step("Получаем видео Youtube с индексом {i}")
    default VertisElement getYoutubeVideo(int i) {
        return youtubeVideosList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список видео Журнала")
    @FindBy("//a[contains(@class,'VideoJournalSectionMobile__item')]")
    ElementsCollection<VertisElement> journalVideosList();

    @Step("Получаем видео Журнала с индексом {i}")
    default VertisElement getJournalVideo(int i) {
        return journalVideosList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список статей Журнала")
    @FindBy("//a[@class = 'VideoRelatedArticlesSection__articleTitle']")
    ElementsCollection<VertisElement> journalArticlesList();

    @Step("Получаем статью Журнала с индексом {i}")
    default VertisElement getJournalArticle(int i) {
        return journalArticlesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Тайтл блока видео Youtube")
    @FindBy("//h2[@class = 'PageVideoMobileDumb__youtubeTitle']")
    VertisElement youtubeVideoBlockTitle();

    @Name("Тайтл блока статей Журнала")
    @FindBy("//h2[@class = 'VideoRelatedArticlesSection__title']")
    VertisElement journalArticlesBlockTitle();
}
