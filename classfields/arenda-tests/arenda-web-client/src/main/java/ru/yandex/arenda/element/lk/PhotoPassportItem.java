package ru.yandex.arenda.element.lk;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Button;

public interface PhotoPassportItem extends Button {

    String LOAD_BUTTON = "Загрузить";
    String PERSONAL_DATA_INPUT = "Личные данные";
    String REGISTRATION_INPUT = "Разворот с пропиской";
    String WITH_SELFIE_INPUT = "Фото с паспортом";

    @Name("Инпут фотки")
    @FindBy(".//input[@type='file']")
    AtlasWebElement photoInput();
}
