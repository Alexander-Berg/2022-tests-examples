package ru.auto.tests.desktop.mobile.element.main;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface StoriesGallery extends VertisElement, WithButton {

    @Name("Кнопка закрытия")
    @FindBy(".//div[contains(@class, 'StorySlide__closer')]")
    VertisElement closeButton();

    @Name("Список пунктов в прогресс баре")
    @FindBy(".//div[contains(@class, 'StoriesGallery__slide_active')]" +
            "/ol[contains(@class, 'StoryProgressBar')]" +
            "/li[contains(@class, 'StoryProgressBar__indicator')]")
    ElementsCollection<VertisElement> progressBarItems();
}