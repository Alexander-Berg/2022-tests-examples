package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Popup extends VertisElement, RadioButton, Input, Button, Checkbox, Link {

    String ADD = "Добавить";
    String CANCEL = "Отмена";

    @Name("Закрыть попап")
    @FindBy(".//div[contains(@class, 'closeButton')]")
    VertisElement close();

    @Name("Контент попапа")
    @FindBy(".//div[contains(@class, 'modalContent')]/div")
    VertisElement content();

    @Name("Айтем меню «{{ value }}»")
    @FindBy(".//div[contains(@class, 'MenuItem')][contains(., '{{ value }}')]")
    VertisElement menuItem(@Param("value") String value);

    @Name("Тултип о забаненности в чатах")
    @FindBy("//div[contains(@class, 'ChatButtonBannedTooltip')]")
    VertisElement bannedInChatsTooltip();

    @Name("Список адресов")
    @FindBy(".//div[contains(@class, '_address')]/span")
    ElementsCollection<VertisElement> addressList();

    @Name("Карта")
    @FindBy(".//ymaps[contains(@class, 'places')]")
    Map map();

    @Name("Название")
    @FindBy(".//span[contains(@class, '_title')]")
    VertisElement title();

    @Name("Текст")
    @FindBy(".//span[contains(@class, '_subText')]")
    VertisElement text();

}
