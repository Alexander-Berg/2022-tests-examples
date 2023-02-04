package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface TechSection extends VertisElement, WithButton, WithRadioButton, WithInput, WithCheckbox {

    @Name("Список годов")
    @FindBy("//span[contains(@class, 'YearField__item')]")
    ElementsCollection<VertisElement> yearsList();

    @Name("Тип кузова {{ bodyType }}")
    @FindBy(".//div[contains(@class, 'BodyTypeField') and .='{{ bodyType }}']")
    VertisElement bodyType(@Param("bodyType") String bodyType);

    @Name("Цвет {{ color }}")
    @FindBy(".//div[contains(@class, '_item') and .='{{ color }}']")
    VertisElement color(@Param("color") String color);

    @Name("Заголовок секции")
    @FindBy("./div[contains(@class, 'TechAccordionSectionHeader')]")
    VertisElement header();

}
