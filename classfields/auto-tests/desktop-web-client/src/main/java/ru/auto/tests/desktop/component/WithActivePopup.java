package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Popup;

public interface WithActivePopup {

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'popup_visible')] | " +
            "//div[contains(@class, 'Popup_visible') or contains(@class, 'Modal_visible')]")
    Popup activePopup();

    @Name("Иконка закрытия поп-апа")
    @FindBy("//div[contains(@class, 'popup_visible')]//div[contains(@class, 'modal__close')] " +
            "| //div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__closer')]")
    VertisElement activePopupCloser();

    @Name("Ссылка в поп-апе")
    @FindBy("(//div[contains(@class,'popup_visible')])[last()]//a " +
            "| (//div[contains(@class,'Popup_visible')])[last()]//a")
    VertisElement activePopupLink();

    @Name("Ссылка в поп-апе")
    @FindBy("(//div[contains(@class,'Popup_visible')])[last()]//a[contains(.,'{{ text }}')] | " +
            "(//div[contains(@class,'popup_visible')])[last()]//a[contains(.,'{{ text }}')]")
    VertisElement activePopupLink(@Param("text") String linkText);

    @FindBy("(//div[contains(@class,'popup_visible')])[last()]//*[contains(@class,'menu-item') and contains(.,'{{ text }}')] | " +
            "//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'MenuItem') and contains(., '{{ text }}')]")
    @Name("Список меню: элемент {{ text }}")
    VertisElement activeListItemByContains(@Param("text") String linkText);

    @Name("Картинка в поп-апе")
    @FindBy("(//div[contains(@class, 'Popup_visible') or contains(@class, 'popup_visible')])[last()]" +
            "//*[contains(@class, 'SaleServicesPopup__content__image') or contains(@class, 'promo-sale__image ')]")
    VertisElement activePopupImage();
}
