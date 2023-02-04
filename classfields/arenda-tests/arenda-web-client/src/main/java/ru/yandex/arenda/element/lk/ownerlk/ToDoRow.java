package ru.yandex.arenda.element.lk.ownerlk;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Link;

public interface ToDoRow extends Link {

    String ADD_PHOTO = "Фото квартиры";
    String ADD_LINK = "Добавить";

    @Name("Галочка")
    @FindBy(".//button[contains(@class,'TodoNotificationItem__success')]")
    AtlasWebElement checkMark();
}
