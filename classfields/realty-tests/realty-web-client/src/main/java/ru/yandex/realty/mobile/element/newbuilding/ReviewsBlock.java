package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.Link;

public interface ReviewsBlock extends Button, Link {

    String ADD_REVIEW = "Написать отзыв";

    @Name("Список отзывов")
    @FindBy(".//div[@class='ReviewsItem']")
    ElementsCollection<Review> reviewsList();
}
