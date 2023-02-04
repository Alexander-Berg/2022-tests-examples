package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface ProfessionalSellerBanner extends VertisElement, WithInput, WithButton {

    String MORE_DETAILS = "Подробнее";

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//button[contains(@class, 'ResellerPublicProfilePromo__closer')]")
    VertisElement closeIcon();

}
