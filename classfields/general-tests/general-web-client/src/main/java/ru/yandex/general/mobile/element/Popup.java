package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Map;

public interface Popup extends VertisElement, Link, Button, Input, Checkbox, RadioButton {

    String SELECTED = "_selected_";

    @Name("Кнопка «Назад» в хэдере попапа")
    @FindBy(".//div[contains(@class,'Screen__baseHeader')]//button[contains(@class, 'BackButton')]")
    VertisElement closePopup();

    @Name("Крестик закрытия всплывающего попапа")
    @FindBy(".//button[contains(@class, 'NativeButton')]")
    VertisElement closeFloatPopup();

    @Name("Строчка с чекбоксом «{{ value }}»")
    @FindBy(".//label[contains(@class, 'FilterCheckboxSetScreen__label')][.='{{ value }}']")
    Checkbox item(@Param("value") String value);

    @Name("Айтем меню «{{ value }}»")
    @FindBy(".//div[contains(@class, 'MenuItem')][contains(., '{{ value }}')]")
    VertisElement menuItem(@Param("value") String value);

    @Name("Список айтемов меню")
    @FindBy(".//div[contains(@class, 'MenuItem')]")
    ElementsCollection<VertisElement> menuItems();

    @Name("Айтем модалки «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Modal__item')][contains(., '{{ value }}')]")
    VertisElement modalItem(@Param("value") String value);

    @Name("Кнопка назад")
    @FindBy(".//button[contains(@class, 'BackButton__button')]")
    VertisElement backButton();

    @Name("Попап забаненности в чатах")
    @FindBy(".//span[contains(@class, 'ChatButtonBannedModal')]")
    VertisElement chatBannedModal();

    @Name("Список тайтлов")
    @FindBy(".//span[contains(@class, '_title_')]")
    ElementsCollection<VertisElement> titleList();

    @Name("Список текстов")
    @FindBy(".//span[contains(@class, '_text_')]")
    ElementsCollection<VertisElement> textList();

    @Name("H3")
    @FindBy(".//span[contains(@class, 'h3')]")
    VertisElement h3();

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'ModalMobile__header')]//span")
    VertisElement title();

    @Name("Список адресов")
    @FindBy(".//div[contains(@class, '_addressItem')]")
    ElementsCollection<VertisElement> addressList();

    @Name("Карта")
    @FindBy(".//ymaps[contains(@class, 'places')]")
    Map map();

    @Name("Блок информации о юзере")
    @FindBy(".//div[contains(@class, '_userBlock')]")
    UserBlock userBlock();

    @Name("Оффер в попапе")
    @FindBy(".//div[contains(@class, 'OffersProlongationModal__offerWrap')]")
    OfferWrap offer();

    @Name("Статистика в попапе")
    @FindBy(".//div[contains(@class, 'StatsModal__modalContent')]")
    StatsModal stats();

    @Name("Чипсина «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Chip__sizeS')][contains(., '{{ value }}')]")
    Chips chips(@Param("value") String value);

    default int getItemsCount() {
        return menuItems().size();
    }

}
