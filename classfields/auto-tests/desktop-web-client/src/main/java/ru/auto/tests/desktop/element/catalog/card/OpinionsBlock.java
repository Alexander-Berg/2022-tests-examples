package ru.auto.tests.desktop.element.catalog.card;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OpinionsBlock extends VertisElement {

    @Name("Кнопка «Добавить отзыв»")
    @FindBy(".//a[text() = 'Добавить отзыв']")
    VertisElement addOpinion();

    @Name("Кнопка «Смотреть все»")
    @FindBy(".//a[text() = 'Смотреть все']")
    VertisElement showAllOpinions();

    @Name("Список отзывов")
    @FindBy(".//li[contains(@class, 'carousel__item')]")
    ElementsCollection<OpinionItem> opinionsList();
}
