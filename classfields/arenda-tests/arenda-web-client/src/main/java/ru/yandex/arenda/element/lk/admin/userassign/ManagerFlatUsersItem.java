package ru.yandex.arenda.element.lk.admin.userassign;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ManagerFlatUsersItem extends AtlasWebElement{

    @Name("Ссылка на пользователя")
    @FindBy(".//a[contains(@class,'ManagerUserBaseItem__link')]")
    AtlasWebElement userLink();

    @Name("Роль привязанного пользователя")
    @FindBy(".//div[contains(@class,'ManagerUserBaseItem__role')]")
    AtlasWebElement assignedRole();

    @Name("Кнопка удаления юзера из привязки")
    @FindBy(".//button[contains(@class,'ManagerAssignedUsers__button') and contains(@class, 'Button_view_red')]")
    AtlasWebElement deleteButton();
}
