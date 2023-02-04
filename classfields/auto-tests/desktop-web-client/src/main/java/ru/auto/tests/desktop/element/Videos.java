package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Videos extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//h2 | " +
            ".//div[contains(@class, '__header')]")
    VertisElement title();

    @Name("Ссылка заголовка")
    @FindBy(".//a[contains(@class, '__titleLink')] | " +
            ".//a[contains(@class, '__header')]")
    VertisElement titleUrl();

    @Name("Список видео")
    @FindBy(".//div[contains(@class, 'video-list__item')] | " +
            ".//div[@class = 'RelatedVideoItem'] | " +
            ".//div[contains(@class, 'VideoCarousel__item')] | " +
            ".//div[@class = 'RelatedVideoItem RelatedVideoItem_size_s'] | " +
            ".//div[contains(@class, 'CardRelatedVideos__item')]")
    ElementsCollection<VertisElement> videosList();

    @Step("Получаем видео с индексом {i}")
    default VertisElement getVideo(int i) {
        return videosList().should(hasSize(greaterThan(i))).get(i);
    }
}