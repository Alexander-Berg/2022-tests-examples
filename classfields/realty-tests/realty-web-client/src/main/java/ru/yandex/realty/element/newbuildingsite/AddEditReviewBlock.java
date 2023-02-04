package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface AddEditReviewBlock extends Button, SelectionBlock {

    String ADD_REVIEW = "Оставить\u00A0отзыв";

    @Name("Поле ввода отзыва")
    @FindBy(".//textarea")
    AtlasWebElement inputField();
}
