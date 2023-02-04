package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.element.cabinet.vin.ValidationFilters;
import ru.auto.tests.desktop.element.cabinet.vin.ValidationResult;
import ru.auto.tests.desktop.page.BasePage;

public interface CabinetReportValidatorPage extends BasePage, WithInput, WithButton {

    @Name("Инпут файла")
    @FindBy("//input[@type = 'file']")
    VertisElement inputFile();

    @Name("Полный статус загрузки")
    @FindBy("//div[@class = 'VinReportValidatorFileUploader__description']")
    VertisElement statusDescription();

    @Name("Статус загрузки")
    @FindBy("//div[@class = 'VinReportValidatorFileUploader__status' and . = '{{ text }}']")
    VertisElement status(@Param("text") String text);

    @Name("Блок с фильтрами результатов")
    @FindBy("//div[contains(@class, 'VinReportValidator__filters')]")
    ValidationFilters validationFilters();

    @Name("Блок «Результаты проверки»")
    @FindBy("//div[@class = 'VinReportValidator__results']")
    ValidationResult validationResult();
}