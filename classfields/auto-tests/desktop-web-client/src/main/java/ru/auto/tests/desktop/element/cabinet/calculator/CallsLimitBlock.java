package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CallsLimitBlock extends VertisElement {

    @Name("Иконка редактирования")
    @FindBy(".//div[contains(@class, '_editIcon')]")
    VertisElement edit();

    @Name("Лимит расходов на звонки")
    @FindBy(".//div[contains(@class, '_limitInfo')]")
    VertisElement limitInfo();

}
