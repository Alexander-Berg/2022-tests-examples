package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface SiteReview extends Button, Link {

    String EDIT = "Редактировать";
    String DELETE = "Удалить";

    @Name("Имя")
    @FindBy(".//div[@class = 'ReviewsItem__username']")
    AtlasWebElement name();

    @Name("Время публикации")
    @FindBy(".//div[@class = 'ReviewsItem__date']")
    AtlasWebElement date();

    @Name("Текст отзыва")
    @FindBy(".//div[@class = 'Shorter']")
    AtlasWebElement text();
}
