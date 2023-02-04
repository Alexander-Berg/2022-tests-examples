package ru.auto.tests.desktop.element.cabinet.dashboard;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.01.19
 */
public interface PromocodePopup extends VertisElement {

    @Name("Содержимое поп-апа")
    @FindBy(".//div[contains(@class, 'PromocodeModal__container')]")
    VertisElement content();

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'PromocodeModal__title')]")
    VertisElement title();

    @Name("Введите промокод")
    @FindBy(".//label//input")
    VertisElement inputPromocode();

    @Name("Кнопка «Активировать»")
    @FindBy(".//button")
    VertisElement activate();

    @Name("Закрыть")
    @FindBy(".//button[./span[contains(., 'Закрыть')]]")
    VertisElement close();

    @Name("Ошибка")
    @FindBy(".//span[@class = 'TextInput__error']")
    VertisElement error();

    @Name("Крест закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'Modal__closer')]")
    VertisElement closePopupIcon();

    @Step("Вводим промокод {promocode}")
    default void inputPromocode(String promocode) {
        inputPromocode().click();
        inputPromocode().clear();
        inputPromocode().sendKeys(promocode);
    }
}
