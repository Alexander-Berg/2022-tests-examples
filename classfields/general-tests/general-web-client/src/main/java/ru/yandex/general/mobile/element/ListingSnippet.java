package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ListingSnippet extends Link, Button, Image {

    @Name("Кнопка добавления в избранное")
    @FindBy(".//button[contains(@class, 'favoriteButton')]")
    VertisElement addToFavorite();

    @Name("Иконка «18+»")
    @FindBy(".//*[contains(@class, '_ageIcon_')]")
    VertisElement adultAgeIcon();

    @Name("Иконка «VAS»")
    @FindBy(".//div[contains(@class, 'VASBadge__raise')]")
    VertisElement vasBadge();

    @Name("Тайтл сниппета")
    @FindBy(".//div[contains(@class, 'title')]")
    VertisElement title();

    @Name("Цена")
    @FindBy(".//div[contains(@class, '_price')]")
    VertisElement price();

    @Name("Заглушка фото")
    @FindBy(".//img[contains(@class, 'SnippetImagePlaceholder')]")
    VertisElement dummyImg();

    @Name("Бейдж доставки")
    @FindBy(".//span[contains(@class, 'SelfDeliveryBadge__text')]")
    VertisElement deliveryBadge();

    default String getUrl() {
        return link().getAttribute("href");
    }

    default String getOfferId() {
        String offerUrl = getUrl();
        Pattern pattern = Pattern.compile("\\/((card)|(form)|(offer))\\/(.+)\\/");
        Matcher matcher = pattern.matcher(offerUrl);
        matcher.find();
        return matcher.group(5);
    }

}
