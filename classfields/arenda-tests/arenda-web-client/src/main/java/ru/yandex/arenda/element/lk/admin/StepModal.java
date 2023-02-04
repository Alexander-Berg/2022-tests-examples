package ru.yandex.arenda.element.lk.admin;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.common.Label;

public interface StepModal extends ElementById, Button, Input, Label {

    String NEXT_BUTTON = "Далее";
    String BACK_BUTTON = "Назад";
    String DONE_BUTTON = "Готово";

    @Name("Селектор с id=«{{ value }}»")
    @FindBy(".//select[@id='{{ value }}']")
    AtlasWebElement selector(@Param("value") String value);

    @Name("Опция селектора value=«{{ value }}»")
    @FindBy(".//select/option[@value='{{ value }}']")
    AtlasWebElement selectorOptionWithValue(@Param("value") String value);

    @Name("Крестик закрытия модуля")
    @FindBy(".//*[contains(@class,'Modal__close')]")
    AtlasWebElement closeCrossModal();
}
