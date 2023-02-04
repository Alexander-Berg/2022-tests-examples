package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface FloatingContacts extends VertisElement, WithButton {

    @Name("Кнопка «Позвонить»")
    @FindBy("//div[contains(@class, 'card__call')]//button[contains(@class, 'phone-call__show-phone-button')] | " +
            "//div[contains(@class, 'CardFloatPhones__controls')]/button[.= 'Позвонить'] | " +
            "//div[contains(@class, 'OfferFloatPhones__controls')]/button[.= 'Позвонить'] | " +
            "//div[contains(@class, 'OfferAmpFloatPhones__controls')]//button[.= 'Показать телефон']")
    VertisElement callButton();

    @Name("Кнопка «Написать»")
    @FindBy(".//button[contains(@class, 'chatButton')] | " +
            ".//button[.= 'Написать'] | " +
            ".//a[contains(@class, 'OfferAmpFloatPhones__chatButtonWithIcon')]")
    VertisElement sendMessageButton();
}