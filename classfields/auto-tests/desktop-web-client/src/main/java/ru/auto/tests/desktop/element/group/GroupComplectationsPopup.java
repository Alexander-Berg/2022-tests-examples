package ru.auto.tests.desktop.element.group;


import io.qameta.allure.Description;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface GroupComplectationsPopup extends VertisElement, WithButton {

    @Description("Комплектация «{{ text }}»")
    @FindBy("//div[@class = 'CardGroupFilterComplectationItem' and contains(., '{{ text }}')]")
    VertisElement complectation(@Param("text") String Text);

    @Description("Выбранная комплектация")
    @FindBy("//div[contains(@class, 'CardGroupFilterComplectationItem_selected')]/div[contains(@class, 'name')]")
    VertisElement selectedComplectation();
}