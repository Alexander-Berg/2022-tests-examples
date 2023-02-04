package ru.yandex.arenda.element.lk.admin;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ManagerFlatItem extends AtlasWebElement {

    @Name("Ссылка заголовка")
    @FindBy(".//a[contains(@class,'ManagerSearchFlatsItem__title')]")
    AtlasWebElement link();

    @Name("Скелетон")
    @FindBy(".//div[contains(@class,'Skeleton__item')]")
    AtlasWebElement skeletonItem();

    @Name("Ссылка на комментарий консьержа")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsItem__commentLink')]")
    AtlasWebElement conciergeCommentLink();

    @Name("Комментарий консьержа")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsItem__comment-')]")
    AtlasWebElement conciergeComment();
}
