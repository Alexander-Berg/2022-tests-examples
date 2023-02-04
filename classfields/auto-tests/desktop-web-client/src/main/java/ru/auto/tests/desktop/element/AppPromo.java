package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AppPromo extends VertisElement {

    @Name("Ссылка на Google Play")
    @FindBy(".//a[contains(@class, 'AppStoreButton_type_googleplay')]")
    VertisElement googlePlayButton();

    @Name("Ссылка на App Store")
    @FindBy(".//a[contains(@class, 'AppStoreButton_type_appstore')]")
    VertisElement appStoreButton();

    @Name("Ссылка на App Gallery")
    @FindBy(".//a[contains(@class, 'AppStoreButton_type_appgallery')]")
    VertisElement appGalleryButton();

    @Name("Ссылка на QR-код")
    @FindBy(".//li[contains(@class, 'AppStoreButtons__button_qr')]")
    VertisElement qrCodeButton();
}
