package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by vicdev on 26.04.17.
 */
public interface AuthManagementPage extends WebPage {

    @Name("Кнопка «Новое объявление»")
    @FindBy("//a[contains(@href, '/management-new/add/')]")
    AtlasWebElement newOfferButton();

}
