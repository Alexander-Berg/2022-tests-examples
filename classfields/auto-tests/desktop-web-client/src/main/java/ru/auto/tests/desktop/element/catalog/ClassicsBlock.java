package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ClassicsBlock extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//h2")
    VertisElement title();

    @Name("Кнопка «Смотреть все»")
    @FindBy(".//span[text() = 'Смотреть все']")
    VertisElement showAll();

    @Name("Кнопка «Следующая»")
    @FindBy(".//i[contains(@class,'carousel__nav-icon_next')]")
    VertisElement nextButton();

    @Name("Кнопка «Предыдущая»")
    @FindBy(".//i[contains(@class,'carousel__nav-icon_prev')]")
    VertisElement prevButton();

    @Name("Список моделей")
    @FindBy(".//li[contains(@class, 'carousel__item')]")
    ElementsCollection<ClassicsItem> modelsList();
}
