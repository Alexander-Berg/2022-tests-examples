package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.element.lk.CreditsFormBlock;

public interface CreditsForm extends VertisElement, WithInput, WithButton, WithGeoSuggest, WithRadioButton {

    String PASSPORT_DATA = "Паспортные данные";
    String ADDRESS = "Адрес";
    String PLACE_OF_WORK = "Место работы";
    String ADDITIONALLY = "Дополнительно";
    String ADDITIONAL_CONTACT = "Дополнительный контакт";
    String ABOUT_COMPANY = "О компании";
    String NEXT = "Далее";

    @Name("Блок «{{ text }}»")
    @FindBy("//div[contains(@class, 'WizardStepMobile') and .//div[.= '{{ text }}']] |" +
            "//div[contains(@class, 'CreditAccordion__section') and .//div[.= '{{ text }}']]")
    CreditsFormBlock block(@Param("text") String text);

    @Name("Инпут саджеста «{{ text }}»")
    @FindBy("//div[contains(@class, 'WizardStepMobile')]//div[.='{{ text }}']/..//textarea | " +
            "//div[contains(@class, 'WizardStepMobile')]//div[.='{{ text }}']/..//input")
    VertisElement suggestInput(@Param("text") String text);

    @Name("Плавающие контролы")
    @FindBy(".//div[contains(@class, 'WizardControlsMobile')]")
    VertisElement floatingControls();
}
