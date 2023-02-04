package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Videos extends VertisElement, WithButton {

    @Name("Список видео")
    @FindBy(".//div[@class = 'RelatedItems__item'] | " +
            ".//div[contains(@class, 'VideoCarousel__videoThumb')] | " +
            ".//div[@class = 'VideoItem'] | " +
            ".//div[contains(@class, 'CardRelatedVideoItem IndexBlock__item')] | " +
            ".//div[contains(@class, 'RelatedVideoItemMobile IndexBlock__item')]")
    ElementsCollection<VertisElement> videosList();

    @Step("Получаем видео с индексом {i}")
    default VertisElement getVideo(int i) {
        return videosList().should(hasSize(greaterThan(i))).get(i);
    }
}
