package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.element.lk.PhonesListItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SettingsPage extends BasePage, WithInput, WithButton {

    @Name("Список подтвержденных телефонов")
    @FindBy("//label[contains(@class, 'TextInput_disabled')]//input")
    ElementsCollection<PhonesListItem> phonesList();

    @Step("Получаем подтвержденный телефон с индексом {i}")
    default PhonesListItem getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Кнопка добавления телефона")
    @FindBy(".//span[contains(@class, 'MyPhones__phone')]//button")
    VertisElement addPhoneButton();

    @Name("Кнопка авторизации через Яндекс")
    @FindBy(".//span[contains(@class, 'Icon_yandex')]")
    VertisElement yandexButton();

    @Name("Кнопка «Добавить ещё»")
    @FindBy(".//div[contains(@class, 'MyProfile__social-profiles-show-unlinked')]")
    VertisElement addMoreButton();

    @Name("Блок с непривязанными соцсетями")
    @FindBy(".//div[contains(@class, 'MyProfile__social-profiles-unlinked-items')]")
    VertisElement unlinkedNetworksBlock();

    @Name("Кнопка удаления привязанной соцсети")
    @FindBy(".//span[contains(@class, 'MyProfile__social-profile-linked-delete')]")
    VertisElement deleteNetworkButton();

    @Name("Блок «Привяжите соцсеть»")
    @FindBy(".//div[contains(@class, 'MyProfile__social-profiles_no')]")
    VertisElement linkProfileBlock();
}
