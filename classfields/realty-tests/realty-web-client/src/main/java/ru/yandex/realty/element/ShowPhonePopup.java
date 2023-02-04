package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ShowPhonePopup extends AtlasWebElement {

    @Name("Добавить в избранное")
    @FindBy(".//button[contains(@class,'PhoneModalOffer__action')]")
    AtlasWebElement addToFav();

    @Name("Телефоны")
    @FindBy(".//div[contains(@class,'PhoneModalContacts__phone--')]")
    ElementsCollection<AtlasWebElement> phones();

    @Name("Ошибка показа телефона 500")
    @FindBy(".//div[@class='SellerContacts__phone_error']")
    AtlasWebElement phoneError();

    @Name("Значок «Номер защищен»")
    @FindBy(".//*[name()='svg' and contains(@class,'PhoneModalContacts__protectIcon')]")
    AtlasWebElement phoneProtectSign();

}
