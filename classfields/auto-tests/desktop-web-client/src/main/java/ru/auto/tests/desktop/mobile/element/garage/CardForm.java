package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface CardForm extends VertisElement, WithButton, WithInput, WithGeoSuggest {

    String CHOOSE_MARK = "Укажите марку";
    String MARK = "Марка";
    String CHOOSE_MODEL = "Укажите модель";
    String MODEL = "Модель";
    String GENERATION = "Поколение";
    String BODY_TYPE = "Тип кузова";
    String MODIFICATION = "Модификация";
    String CHOOSE_COLOR = "Укажите цвет";
    String SAVE = "Сохранить";

    @Name("Развёрнутый блок «{{ text }}»")
    @FindBy(".//div[contains(@class, 'FormSection_opened') and .//div[contains(@class, 'FormSection__Title') " +
            "and .= '{{ text }}']]")
    CardFormBlock unfoldedBlock(@Param("text") String text);

    @Name("Блок «{{ title }}»")
    @FindBy("//div[@class = 'FormFieldsSelector__Item'][.//div[@class = 'FormSection__Title' and .='{{ title }}']]")
    CardFormBlock block(@Param("title") String title);

    @Name("Кнопка «Сохранить»")
    @FindBy("//div[@class = 'GarageSubmitButton']/button")
    VertisElement submitButton();

}