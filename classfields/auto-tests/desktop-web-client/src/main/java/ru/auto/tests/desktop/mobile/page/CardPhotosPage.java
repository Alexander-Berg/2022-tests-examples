package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardPhotosPage extends BasePage {

    @Name("Форма загрузки фотографий")
    @FindBy("//div[@class = 'FormsApp']")
    VertisElement addPhotosForm();

    @Name("Кнопка «Сохранить»")
    @FindBy("//button[contains(@class, 'OfferPhotosAddForm__saveButton')]")
    VertisElement saveButton();
}