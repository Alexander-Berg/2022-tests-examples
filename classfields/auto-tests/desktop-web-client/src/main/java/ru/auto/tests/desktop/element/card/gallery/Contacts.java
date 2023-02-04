package ru.auto.tests.desktop.element.card.gallery;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Contacts extends VertisElement {

    @Name("Адрес")
    @FindBy(".//span[contains(@class, 'ownerAddress')]")
    VertisElement address();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class, 'CardImageGallery__phoneButton')] | " +
            ".//button[contains(@class, 'phone')]")
    VertisElement showPhoneButton();

    @Name("Кнопка «Написать»")
    @FindBy(".//div[contains(@class, 'PersonalMessage')]")
    VertisElement sendMessageButton();
}
