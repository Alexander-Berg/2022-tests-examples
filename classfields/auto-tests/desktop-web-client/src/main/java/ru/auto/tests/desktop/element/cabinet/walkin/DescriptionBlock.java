package ru.auto.tests.desktop.element.cabinet.walkin;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DescriptionBlock extends VertisElement {

    @Name("Текст описания")
    @FindBy(".//div[@class = 'WalkInHeader__description']")
    VertisElement text();

    @Name("Кнопка «Как это работает»")
    @FindBy(".//div[@class = 'WalkInHeader__promoButton']/a")
    VertisElement promoButton();

    @Name("Кнопка «Закрыть уведомление»")
    @FindBy(".//div[@class = 'WalkInHeader__cancelButton']/button")
    VertisElement cancelButton();
}
