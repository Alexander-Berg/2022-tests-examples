package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.cabinet.calls.PhoneNumber;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetCallsRedirectPhonesPage extends CabinetCallsPage {

    @Name("Список номеров телефонов")
    @FindBy("//tr[.//td]")
    ElementsCollection<PhoneNumber> phoneNumbersList();

    @Step("Получаем звонок с индексом {i}")
    default PhoneNumber getPhoneNumber(int i) {
        return phoneNumbersList().should(hasSize(greaterThan(i))).get(i);
    }
}
