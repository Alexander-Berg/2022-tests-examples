package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithMMMFilter;
import ru.auto.tests.desktop.element.VideoPageBlock;

public interface VideoPage extends BasePage, WithMMMFilter {

    String H1_TEXT = "Тест-драйвы";

    @Name("Блок видео Журнала")
    @FindBy("//div[contains(@class, 'VideoJournalSection')]")
    VideoPageBlock magVideoBlock();

    @Name("Блок «Со всего интернета»")
    @FindBy("//div[@class = 'VideoYoutubeSection']")
    VideoPageBlock youtubeBlock();

    @Name("Блок «По теме в Журнале»")
    @FindBy("//div[@class = 'VideoRelatedArticlesSection']")
    VideoPageBlock articlesBlock();

    @Name("Активное видео")
    @FindBy("//div[contains(@class,'VideoYoutubeSection__videoRow')]")
    VertisElement activeVideo();
}