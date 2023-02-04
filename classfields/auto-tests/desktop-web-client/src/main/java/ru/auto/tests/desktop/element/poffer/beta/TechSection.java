package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;

public interface TechSection extends VertisElement, WithButton, WithRadioButton, WithInput, WithCheckbox {

    String MILEAGE_PLACEHOLDER = "км";

    @Name("Список годов")
    @FindBy("//span[contains(@class, 'YearField__item')]")
    ElementsCollection<VertisElement> yearsList();

    @Name("Тип кузова {{ bodyType }}")
    @FindBy(".//div[contains(@class, 'BodyTypeField__tile') and .='{{ bodyType }}']")
    VertisElement bodyType(@Param("bodyType") String bodyType);

    @Name("Цвет {{ color }}")
    @FindBy(".//div[contains(@class, 'ColorField__item') and @data-id='{{ color }}']")
    VertisElement color(@Param("color") String color);

    @Name("Заголовок секции")
    @FindBy("./div[contains(@class, 'TechAccordionSectionHeader')]")
    VertisElement header();
}
