package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface AllFiltersPopup extends Popup, Input {

    String SHOW_BUTTON = "Показать";

    @Name("Крестик закрытия попапа «Все фильтры»")
    @FindBy(".//div[contains(@class,'ModalDesktop__closeButton')]")
    VertisElement closeAllFiltersPopupButton();

    @Name("Фильтр «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OfferFilterFormField__container')][.//span[.='{{ value }}']]")
    FilterBlock filterBlock(@Param("value") String value);

    @Name("Сбросить")
    @FindBy(".//div[contains(@class,'OfferFilterForm__footer')]/span")
    VertisElement cancel();

    @Name("Футер попапа")
    @FindBy(".//div[contains(@class, 'OfferFilterForm__footer')]")
    Button footer();

    default void show() {
        waitSomething(1, TimeUnit.SECONDS);
        footer().button().click();
    }
}
