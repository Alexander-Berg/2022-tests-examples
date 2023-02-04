package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface AutoruChannel extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'Index__title-link')]")
    VertisElement title();

    @Name("Список видео")
    @FindBy(".//div[@class = 'IndexPlaylists__item Index__col']")
    ElementsCollection<VertisElement> videosList();

    @Name("Ссылка «Все видео»")
    @FindBy(".//a[contains(@class, 'Index__all-link')]")
    VertisElement allVideosUrl();

    @Step("Получаем видео с индексом {i}")
    default VertisElement getVideo(Integer i) {
        return videosList().should(hasSize(greaterThan(i))).get(i);
    }
}