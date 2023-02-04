package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.04.18
 */
public interface EnrollOnCheckPopup extends VertisElement {

    @Name("Тип проверки «{{checkType}}»")
    @FindBy(".//span[contains(@class, 'certification-form__type-switcher')]//button[contains(., '{{ checkType }}')]")
    VertisElement checkType(@Param("checkType") String checkType);

    @Name("Адреса центров проверки")
    @FindBy(".//div[contains(@class, 'certification-address__item certification-form__stationary-point')]")
    ElementsCollection<VertisElement> certAddressItems();

    @Name("Выбрать другой центр")
    @FindBy(".//span[contains(@class, 'certification-form__choose-another')]")
    VertisElement chooseAnotherCenter();

    @Name("Время проверки")
    @FindBy(".//div[contains(@class, 'certification__schedule-hour_')]")
    ElementsCollection<VertisElement> checkingTimes();

    @Name("Свободне время проверки")
    @FindBy(".//div[contains(@class, 'certification__schedule-hour_status_available')]")
    ElementsCollection<VertisElement> availableCheckingTimes();

    @Name("Кнопка «Записаться и оплатить»")
    @FindBy(".//button[contains(@class, 'certification-form__submit')]")
    VertisElement enrollAndPay();

    @Name("Поле «Укажите адрес...»")
    @FindBy(".//div[contains(@class, 'certification-form__address')]//input")
    VertisElement inputAddress();

    @Name("Кнопка «Понятно, спасибо» на нотификация о успешной записи на проверку")
    @FindBy("//div[contains(@class, 'certification-content_type_success')]//button")
    VertisElement successNotificationButton();

    default VertisElement firstAvailableCheckingTime() {
        return availableCheckingTimes().should(hasSize(greaterThan(0))).get(0);
    }
}
