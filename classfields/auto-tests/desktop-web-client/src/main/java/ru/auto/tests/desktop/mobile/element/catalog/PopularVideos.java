package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface PopularVideos extends VertisElement {

    @Name("Список видео")
    @FindBy(".//div[contains(@class,'VideoList__item')] | " +
            ".//div[@class = 'RelatedItems__item'] | " +
            ".//div[contains(@class, 'VideoCarouselItem')] | " +
            ".//div[contains(@class, 'VideoCarousel__item')]")
    ElementsCollection<VertisElement> videosList();

    @Name("Ссылка на все видео")
    @FindBy(".//div[contains(@class, 'RelatedItems__all')]/a | " +
            ".//div[contains(@class, 'VideoCarousel__footer')]/a")
    VertisElement allVideosUrl();

    @Name("Поп-ап видео")
    @FindBy(".//div[@class = 'Video__paranja'] | " +
            "//div[@class = 'VideoItem__paranja'] | " +
            "//amp-youtube")
    VertisElement popup();

    @Step("Получаем видео с индексом {i}")
    default VertisElement getVideo(int i) {
        return videosList().should(hasSize(greaterThan(i))).get(i);
    }
}
