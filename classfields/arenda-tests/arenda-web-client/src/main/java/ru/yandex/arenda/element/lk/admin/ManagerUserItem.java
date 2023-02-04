package ru.yandex.arenda.element.lk.admin;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ManagerUserItem extends AtlasWebElement {

    @Name("Ссылка юзера")
    @FindBy(".//a[contains(@class,'ManagerSearchUsersItem__link')]")
    AtlasWebElement link();

    @Name("Ссылка юзера")
    @FindBy(".//div[contains(@class,'ManagerSearchUsersItem__phone')]")
    AtlasWebElement phone();

    @Name("Скелетон")
    @FindBy(".//div[contains(@class,'Skeleton__item')]")
    AtlasWebElement skeletonItem();

    @Name("Ссылка на комментарий консьержа")
    @FindBy(".//div[contains(@class,'ManagerSearchUsersItem__commentLink')]")
    AtlasWebElement conciergeCommentLink();

    @Name("Комментарий консьержа")
    @FindBy(".//div[contains(@class,'ManagerSearchUsersItem__comment-')]")
    AtlasWebElement conciergeComment();

}
