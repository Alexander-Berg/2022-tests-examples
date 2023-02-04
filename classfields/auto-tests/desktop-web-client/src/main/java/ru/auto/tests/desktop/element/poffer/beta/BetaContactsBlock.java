package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface BetaContactsBlock extends VertisElement, WithInput, WithButton {

    String NAME = "Как к вам обращаться?";
    String EMAIL = "Электронная почта (e-mail)";
    String PHONE_NUMBER = "Номер телефона";
    String CONFIRM_NUMBER = "Подтвердить номер";
    String SMS_CODE = "Код из SMS";

    @Name("Тип связи {{ type }}")
    @FindBy(".//div[@class='OfferFormCommunicationTypeField__tags']//button[.='{{ type }}']")
    VertisElement communicationType(@Param("type") String type);

}