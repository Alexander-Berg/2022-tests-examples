package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.lk.admin.StepModal;

public interface AdminFlatPage extends BasePage, ElementById, Button, Input {

    String FLAT_ADDRESS = "FLAT_ADDRESS";
    String FLAT_STATUS = "FLAT_STATUS";
    String DESIRED_RENT_AMOUNT = "DESIRED_RENT_AMOUNT";
    String OWNER_APPLICATION_NAME = "OWNER_APPLICATION_NAME";
    String OWNER_APPLICATION_SURNAME = "OWNER_APPLICATION_SURNAME";
    String OWNER_APPLICATION_PHONE = "OWNER_APPLICATION_PHONE";
    String OWNER_APPLICATION_EMAIL = "OWNER_APPLICATION_EMAIL";
    String CONCIERGE_COMMENT_ID = "CONCIERGE_COMMENT";

    String SAVE_BUTTON = "Сохранить";
    String ADD_BUTTON = "Добавить";
    String CONTRACTS_BUTTON = "Договоры";

    @Name("Ошибка в поле «{{ value }}» на странице персональных данных")
    @FindBy("//div[contains(@class,'withManagerFormClasses__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputFlatPAge(@Param("value") String value);

    @Name("Элементы саджеста адреса")
    @FindBy(".//ul/li[contains(@class,'SuggestList__item')]")
    ElementsCollection<AtlasWebElement> suggestList();

    @Name("Модуль мобилки по заполнению полей")
    @FindBy(".//div[contains(@class,'StepByStepModal__popup') and contains(@class, 'Modal_visible')]")
    StepModal stepModal();

    @Name("Кнопка «Сохранить» комментарий консьержа")
    @FindBy(".//button[contains(@class,'ConciergeCommentForm__submitButton')]")
    AtlasWebElement conciergeSaveCommentButton();
}
