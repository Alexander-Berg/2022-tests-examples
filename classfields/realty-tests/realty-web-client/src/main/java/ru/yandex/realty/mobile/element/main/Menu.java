package ru.yandex.realty.mobile.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.CloseCross;
import ru.yandex.realty.mobile.element.Link;

public interface Menu extends Link, CloseCross {

    @Name("amp. Ссылка «Недвижимость»")
    @FindBy(".//div[contains(@class,'Logo__service')]/a")
    AtlasWebElement ampRealtyLink();

    @Name("amp. Ссылка «Яндекс»")
    @FindBy(".//a[.//i[contains(@class,'Icon_type_logo')]]")
    AtlasWebElement ampYandexLink();
}
