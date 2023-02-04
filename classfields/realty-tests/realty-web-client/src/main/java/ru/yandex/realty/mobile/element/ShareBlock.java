package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface ShareBlock extends Button {

    String CANCEL = "Отменить";

    @Name("Кнопка «Назад»")
    @FindBy("//span[contains(@class,'NavBar__button')]//i[contains(@class,'Icon_type_share')]")
    AtlasWebElement shareButton();

    @Name("Модуль поделиться")
    @FindBy("//div[contains(@class, 'Modal_visible')][//div[@class='ShareModal']]")
    AtlasWebElement shareModal();
}
