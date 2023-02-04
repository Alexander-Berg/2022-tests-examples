package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Videos;

public interface WithVideos {

    @Name("Популярные видео")
    @FindBy("//div[@class='catalog__section' and contains(., 'Популярные видео')] | " +
            "//div[contains(@class, '__videoCarousel')] | " +
            "//div[contains(@class, 'PageCardGroupAbout__videos')] | " +
            "//div[contains(@class, 'CardRelatedVideos')] | " +
            "//div[contains(@class, 'CardRelatedVideos__container')]")
    Videos videos();
}