package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

import static ru.auto.tests.commons.util.Utils.getRandomString;

public interface LkFeedBackPage extends BasePage {

    String YOUR_FEEDBACK = "Ваш отзыв";
    String HEADER_TEXT_FEED_BACK = "Оцените работу сервиса";

    @Name("Звездочка «{{ value }}»")
    @FindBy(".//div[@data-rating = '{{ value }}']")
    AtlasWebElement star(@Param("value") String value);

    @Name("Поле отзыва")
    @FindBy(".//textarea")
    AtlasWebElement textarea();

    @Name("Отзыв")
    @FindBy(".//div[contains(@class,'UserFeedbackPreview__myReview')]")
    AtlasWebElement myReview();

    @Name("ссылка на изменение")
    @FindBy(".//a[contains(@class,'Link Link_js_inited Link_size_xl Link_theme_islands')]")
    AtlasWebElement editLink();

    default AtlasWebElement sendButton() {
        return button("Отправить");
    }

    default void sendStarredReview(String starCount) {
        star(starCount).click();
        textarea().sendKeys(getRandomString());
        sendButton().click();
    }
}
