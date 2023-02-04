package ru.auto.tests.desktop.element.catalogNew.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface SpecificationBlock extends VertisElement, WithButton {

    @Name("Ccылка «Все характеристики»")
    @FindBy(".//a[@class = 'Link' and . = 'Все характеристики']")
    VertisElement configurationLink();

    @Name("Тумба с фото")
    @FindBy(".//div[@class = 'SpecificationContent__thumb']")
    VertisElement photo();

    @Name("Строка конфигурации")
    @FindBy(".//tr[@class = 'SpecificationContent__tableBodyRow']")
    VertisElement row();

    @Name("Кнопка сравнения")
    @FindBy(".//div[contains(@class, '__buttonCompare')]")
    VertisElement compareButton();

}
