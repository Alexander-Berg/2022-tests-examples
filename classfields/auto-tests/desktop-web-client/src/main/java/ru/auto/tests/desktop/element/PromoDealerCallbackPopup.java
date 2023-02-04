package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface PromoDealerCallbackPopup extends VertisElement, WithInput, WithButton {

    @Name("Результат отправки заявки на обратный звонок")
    @FindBy("//div[contains(@class, 'PromoPageForDealerForm__formSent')]")
    VertisElement requestResult();
}