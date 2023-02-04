package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ListingSnippet extends VertisElement, Link, Button, Image {

    String SHOW_PHONE = "Показать телефон";
    String WRITE = "Написать";

    @Name("Кнопка добавления в избранное")
    @FindBy(".//button[contains(@class, 'favoriteButton')]")
    FavoriteButton addToFavorite();

    @Name("Тайтл сниппета")
    @FindBy(".//div[contains(@class, 'title')]")
    VertisElement title();

    @Name("Цена")
    @FindBy(".//div[contains(@class, '_price')]")
    VertisElement price();

    @Name("Адрес сниппета")
    @FindBy(".//span[contains(@class, 'address')]")
    VertisElement address();

    @Name("Иконка «18+»")
    @FindBy(".//*[contains(@class, '_ageIcon_')]")
    VertisElement adultAgeIcon();

    @Name("Иконка «VAS»")
    @FindBy(".//div[contains(@class, 'VASBadge__raise')]")
    VertisElement vasBadge();

    @Name("Заглушка фото")
    @FindBy(".//img[contains(@class, 'SnippetImagePlaceholder')]")
    VertisElement dummyImg();

    @Name("Бейдж доставки")
    @FindBy(".//span[contains(@class, 'SelfDeliveryBadge__text')]")
    VertisElement deliveryBadge();

    @Name("Ссылка с телефоном")
    @FindBy(".//a[contains(@class, 'PhoneButton__link')]")
    Link phoneLink();

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
