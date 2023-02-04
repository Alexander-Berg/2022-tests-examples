package ru.auto.tests.desktop.element.cabinet.users;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GroupBlock extends VertisElement, WithInput {

    @Name("Название группы")
    @FindBy(".//div[contains(@class, 'title')]")
    VertisElement title();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'button__text') and .='{{ text }}'] | " +
            ".//span[contains(@class, 'Button__content') and .='{{ text }}']")
    VertisElement button(@Param("text") String Text);

    @Name("Ссылка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Link') and .='{{ text }}']")
    VertisElement link(@Param("text") String Text);

    @Name("Список доступов")
    @FindBy(".//span[contains(@class, 'UsersRolesGroup__grantList')]")
    ElementsCollection<VertisElement> accessList();

    @Name("Список пользователей")
    @FindBy(".//div[contains(@class, 'UsersRolesListingItem__userItem')]")
    ElementsCollection<UserBlock> usersList();

    @Name("Пользователь «{{ text }}»")
    @FindBy(".//div[contains(@class, 'UsersRolesUserItem__name') and .='{{ text }}']")
    VertisElement user(@Param("text") String Text);

    @Step("Получаем пользователя с индексом {i}")
    default UserBlock getUser(int i) {
        return usersList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Доступ с индексом {i}")
    default VertisElement getAccessList(int i) {
        return accessList().should(hasSize(greaterThan(i))).get(i);
    }
}
