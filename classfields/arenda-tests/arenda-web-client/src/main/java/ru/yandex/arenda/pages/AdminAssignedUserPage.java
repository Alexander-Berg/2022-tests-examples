package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.lk.admin.userassign.ManagerFlatUsersItem;
import ru.yandex.arenda.element.lk.admin.userassign.SuggestListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface AdminAssignedUserPage extends BasePage, ElementById, Button, Input {

    String USER_SUGGEST_ID = "USER_SUGGEST";
    String ASSIGN_BUTTON = "Привязать";
    String UNASSIGN_BUTTON = "Отвязать";

    @Name("Элементы саджеста пользователей")
    @FindBy("//ul/li[contains(@class,'SuggestList__item')]")
    ElementsCollection<SuggestListItem> suggestList();

    default AtlasWebElement suggestElement(String name) {
        return suggestList().waitUntil(hasSize(greaterThan(0)))
                .filter(element -> element.userName().getText().equals(name)).get(0);
    }

    @Name("Привязанный пользователь")
    @FindBy(".//div[contains(@class,'ManagerAssignedUsers__snippet')]")
    ManagerFlatUsersItem managerFlatUsersSnippet();

    @Name("Второй привязанный пользователь")
    @FindBy(".//div[contains(@class,'ManagerAssignedUsers__snippet')][1]")
    ManagerFlatUsersItem managerFlatUsersSnippet2();

    @Name("Контейнер привязанных пользователей")
    @FindBy(".//div[contains(@class,'ManagerAssignedUsers__container')]")
    AtlasWebElement assignedUsersContainer();

    @Name("Селектор генерации ссылки кандидат/собственник")
    @FindBy(".//div[contains(@class,'ManagerUserAssignmentLink__item')]//select")
    AtlasWebElement assignUrlSelector();

    @Name("Селектор роли при привязке собственник/жилец/кандидат")
    @FindBy(".//select[@id='USER_ROLE']")
    AtlasWebElement assignRolSelector();

    @Name("Строчка селектора «{{ value }}»")
    @FindBy(".//option[contains(.,'{{ value }}')]")
    AtlasWebElement selectorOption(@Param("value") String value);

    @Name("Кнопка «Скопировать ссылку»")
    @FindBy(".//button[contains(@data-test,'ManagerUserAssignmentLink__copyButton')]")
    AtlasWebElement copyLinkButton();

    @Name("Попап подтверждения отвязки")
    @FindBy(".//div[contains(@class,'Modal_visible')]")
    Button unassignModal();
}
