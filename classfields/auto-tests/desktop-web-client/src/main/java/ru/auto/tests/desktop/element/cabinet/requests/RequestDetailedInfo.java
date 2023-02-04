package ru.auto.tests.desktop.element.cabinet.requests;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface RequestDetailedInfo extends VertisElement {

    @Name("Кнопка закрытия блока")
    @FindBy(".//div[@class = 'MatchApplicationsDetails__closeIcon']")
    VertisElement closeButton();

    @Name("Контент блока")
    @FindBy(".//div[@class = 'MatchApplicationsDetailsContent']")
    VertisElement content();

    @Name("Номер телефона")
    @FindBy(".//div[@class = 'MatchApplicationsDetailsContent__phone']")
    VertisElement phoneNumber();
}