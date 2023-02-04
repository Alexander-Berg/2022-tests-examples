package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.element.Popup;

public interface CreditsForm extends VertisElement, WithInput, WithButton, WithGeoSuggest {

    String PASSPORT_DATA = "Паспортные данные";
    String ADDRESS = "Адрес";
    String PLACE_OF_WORK = "Место работы";
    String ADDITIONALLY = "Дополнительно";
    String ADDITIONAL_CONTACT = "Дополнительный контакт";
    String DRIVING_LICENSE = "Водительское удостоверение";
    String NEXT = "Далее";


    @Name("Блок «{{ text }}»")
    @FindBy("//div[contains(@class, 'CreditAccordion__section ') and .//div[.= '{{ text }}']]")
    CreditsFormBlock block(@Param("text") String text);

    @Name("Плавающие контролы")
    @FindBy(".//div[contains(@class, 'WizardControlsDesktop')]")
    VertisElement floatingControls();

    @Name("Блок «Сумма кредита»")
    @FindBy(".//div[contains(@class, '_field_amount')]")
    CreditPopupBlock amountBlock();

    @Name("Блок «Срок кредита»")
    @FindBy(".//div[contains(@class, '_term')]")
    CreditPopupBlock termBlock();

}
