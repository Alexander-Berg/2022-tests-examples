package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.Videos;

public interface WithVideos {

    @Name("Популярные видео")
    @FindBy("//div[@class = 'RelatedItems' and descendant::text() = 'Популярные видео'] | " +
            "//div[contains(@class, 'SliderListing-module__container VideoCarousel')] | " +
            "//div[contains(@class, 'RelatedItems RelatedItems__border')] | " +
            "//div[@class = 'RelatedItems' and descendant::text() = 'Видео о модели'] | " +
            "//div[@class = 'LazyComponent' and .//a[.= 'Видео о модели']] | " +
            "//div[contains(@class, 'CardRelatedVideo')] | " +
            "//div[contains(@class, 'PageCardGroupAbout__videoContainer')]")
    Videos videos();

    @Name("Фрейм с видео")
    @FindBy("//div[@class = 'Video__paranja'] | " +
            "//div[@class = 'VideoItem__paranja'] | " +
            "//div[@class = 'VideoModalM__paranja']")
    VertisElement videoFrame();

    @Name("Иконка закрытия фрейма с видео")
    @FindBy("//div[@class = 'VideoItem__close'] | " +
            "//div[@class = 'VideoModalM__close']")
    VertisElement videoCloseIcon();
}