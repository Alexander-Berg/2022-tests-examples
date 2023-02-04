package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ReviewBlock extends Link, Button {

    String FOR_DEVELOPER = "Для застройщика";
    String SEND_ANONYMUS = "Отправить\u00A0анонимно";

    @Name("Блок добавления отзыва")
    @FindBy(".//div[@class='ReviewsForm__controls']")
    AddEditReviewBlock reviewArea();

    @Name("Список отзывов")
    @FindBy(".//div[@class = 'ReviewsItem']")
    ElementsCollection<SiteReview> siteReviewList();

    default SiteReview siteReview(int i) {
        return siteReviewList().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
