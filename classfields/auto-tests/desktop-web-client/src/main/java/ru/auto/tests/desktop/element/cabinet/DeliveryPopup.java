package ru.auto.tests.desktop.element.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.mobile.element.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DeliveryPopup extends VertisElement, WithInput, WithGeoSuggest {

    @Name("Ссылка «{{ text }}»")
    @FindBy(".//a[contains(@class, 'Link') and .= '{{ text }}']")
    VertisElement url(@Param("text") String name);

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//button[.= '{{ text }}']")
    VertisElement button(@Param("text") String name);

    @Name("Список адресов")
    @FindBy(".//ul[contains(@class, 'DeliverySettingsRegions__inner')]")
    ElementsCollection<DeliveryPopupAddress> addressList();

    @Step("Получаем адрес с индексом {i}")
    default DeliveryPopupAddress getAddress(int i) {
        return addressList().should(hasSize(greaterThan(i))).get(i);
    }
}
