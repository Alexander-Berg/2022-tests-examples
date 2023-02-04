package ru.auto.tests.desktop.element.cabinet.vin;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface ValidationResult extends VertisElement, WithInput, WithButton {

    @Name("Блок «Результаты проверки»")
    @FindBy(".//div[@id = 'VinReportValidatorResult']")
    ElementsCollection<VertisElement> results();

    @Name("Блок «Статистика по VIN»")
    @FindBy(".//div[contains(@class, 'VinReportValidatorCounters__container')]")
    VertisElement statistics();

    @Name("Ошибки и предупреждения")
    @FindBy(".//div[@class = 'VinReportValidatorResult__itemTitle']")
    VertisElement errors();
}
