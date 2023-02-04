package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.cabinet.users.AccessBlock;
import ru.auto.tests.desktop.element.cabinet.users.EditPopup;
import ru.auto.tests.desktop.element.cabinet.users.GroupBlock;
import ru.auto.tests.desktop.element.cabinet.users.NewUserBlock;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetUsersPage extends BasePage, WithNotifier, WithInput, WithSelect {

    @Name("Страница «Пользователи»")
    @FindBy(".//div[contains(@class, 'Users__container')]")
    VertisElement usersPage();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Button__text') and .='{{ text }}']")
    VertisElement button(@Param("text") String Text);

    @Name("Переключатель «{{ text }}»")
    @FindBy("//button[contains(@class, 'button_togglable_radio') and .= '{{ text }}']")
    VertisElement toggle(@Param("text") String Text);

    @Name("Блок «Доступ»")
    @FindBy(".//div[@class = 'UsersRolesEditRoleForm__container']")
    AccessBlock accessBlock();

    @Name("«Новый пользователь кабинета»")
    @FindBy("//div[contains(@class, 'UsersRolesAddUserForm__container')]")
    NewUserBlock newUserBlock();

    @Name("Список групп")
    @FindBy(".//div[contains(@class, 'UsersRolesListing__item')]")
    ElementsCollection<GroupBlock> groupsList();

    @Name("Поп-ап «Редактирование»")
    @FindBy("//div[contains(@class, ' Popup_js_inited UsersRolesUserItem__editPopup')]")
    EditPopup editPopup();

    @Name("Группа «{{ text }}»")
    @FindBy(".//div[contains(@class, 'UsersRolesGroup__title') and .='{{ text }}']")
    VertisElement group(@Param("text") String Text);

    @Step("Получаем группу с индексом {i}")
    default GroupBlock getGroup(int i) {
        return groupsList().should(hasSize(greaterThan(i))).get(i);
    }
}
