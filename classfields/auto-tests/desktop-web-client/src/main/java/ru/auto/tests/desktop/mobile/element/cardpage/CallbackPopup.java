package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CallbackPopup extends VertisElement, WithInput, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'ModalHeader')]")
    VertisElement title();

    @Name("Кнопка «Закрыть»")
    @FindBy(".//button[contains(@class, 'modal__close')]")
    VertisElement closeButton();

    @Name("Сообщение об успешной отправке заявки")
    @FindBy(".//div[@class='sale-callback-popup__success']")
    VertisElement successMessage();

    @Name("Саджест телефонов")
    @FindBy(".//div[@class = 'RichInput__suggest']")
    VertisElement phonesSuggest();

    @Name("Список телефонов в саджесте")
    @FindBy(".//div[contains(@class, 'RichInput__suggest-item')]")
    ElementsCollection<VertisElement> phonesList();

    @Name("Пользовательское соглашение")
    @FindBy("//div[contains(@class, 'Modal_visible') and .//div[contains(@class, 'termsContent')]]")
    VertisElement userAgreement();

    @Name("Кнопка очистки инпута")
    @FindBy(".//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement clearInputButton();

    @Step("Получаем телефон с индексом {i}")
    default VertisElement getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }
}