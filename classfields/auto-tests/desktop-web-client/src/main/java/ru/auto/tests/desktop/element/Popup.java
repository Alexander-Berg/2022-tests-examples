package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithSpinner;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface Popup extends VertisElement, WithInput, WithButton, WithSelect, WithSpinner, WithCheckbox, WithGeoSuggest {

    String AGREE = "Согласен";
    String NO_THANKS = "Нет, спасибо";
    String UNDERSTAND_THANKS = "Понятно, спасибо";

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'ModalDialogCloser')] | " +
            "..//div[contains(@class, 'Modal__closer')] | " +
            ".//div[contains(@class, 'CloseButton')]")
    VertisElement closeIcon();

    @Name("Кнопка «У меня вопрос»")
    @FindBy(".//a[contains(@class, 'OfferComplaintPopupDesktop__helpLink')]")
    VertisElement helpButton();
}
