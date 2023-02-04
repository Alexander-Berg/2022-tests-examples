package ru.auto.tests.desktop.element.cabinet.agency;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.09.18
 */
public interface WithPopup {

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'popup_visible')]")
    VertisElement commonPopup();

    @Name("Выбираем в поп-апе «{{ value }}»")
    @FindBy("//div[contains(@class,'popup_visible')]//div[contains(@class,'menu-item') and contains(.,'{{ value }}')]")
    VertisElement popupSelect(@Param("value") String value);
}
