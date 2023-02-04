package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.management.StickyTariff;
import ru.yandex.realty.element.management.TariffPopup;

public interface TariffsPage extends BasePage, Link {

    @Name("Плавающий блок")
    @FindBy("//div[contains(@class, 'TuzTariffsStickyList')]")
    StickyTariff stickyTariff();

    @Name("Попап переключения тарифа")
    @FindBy("//div[contains(@class, 'Modal_visible TuzTariffChangePopup__popup')]//div[@class='Modal__content']")
    TariffPopup tariffPopup();

    @Name("Информер «{{ value }}»")
    @FindBy(".//i[@aria-label='{{ value }}']")
    AtlasWebElement informer(@Param("value") String value);

    @Name("Попапчик информера")
    @FindBy(".//div[contains(@class,'Popup_visible')]")
    AtlasWebElement informerPopup();

    @Name("Тариф успешно изменен")
    @FindBy(".//div[contains(@class,'header-alert header-alert_animated')]")
    AtlasWebElement animated();
}
