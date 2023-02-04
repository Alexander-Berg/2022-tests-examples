package ru.yandex.realty.mobile.element.listing;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.mobile.element.Link;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TouchOffer extends Link, ButtonWithTitle, RealtyElement, Button {

    String CALL_BUTTON = "Позвонить";
    String ADD_TO_FAVORITES = "Добавить в избранное";
    String DELETE_FROM_FAVORITES = "Удалить из избранного";
    String TUZ_ICON = "tuz";

    @Name("Тэг в описании «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OfferSerpSnippet__badges')]/div[contains(.,'{{ value }}')]")
    AtlasWebElement tag(@Param("value") String value);

    @Name("Тэг цены «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OfferSerpSnippetPricePanel__profit') and contains(.,'{{ value }}')]")
    AtlasWebElement priceTag(@Param("value") String value);

    @Name("Тэг в галерее «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OfferSerpSnippet__galleryBadges')]/div[contains(.,'{{ value }}')]")
    AtlasWebElement galleryTag(@Param("value") String value);

    @Name("Тэг Аренды в галерее")
    @FindBy(".//div[contains(@class, 'OfferSerpSnippet__galleryBadges')]/div[contains(@class, 'Badge__view_purple_arenda')]")
    AtlasWebElement galleryArendaTag();

    @Name("Иконка vas «{{ value }}»")
    @FindBy(".//div[contains(@class, 'VasIcon_type_{{ value }}')]")
    AtlasWebElement vasIcon(@Param("value") String value);

    @Name("Иконка увеличения цены")
    @FindBy(".//i[contains(@class, 'increased')]")
    RealtyElement increasedPrice();

    @Name("Иконка уменьшения цены")
    @FindBy(".//i[contains(@class, 'decreased')]")
    AtlasWebElement decreasedPrice();

    @Name("Похожее объявление")
    @FindBy(".//div[contains(@class, 'inexact-match')]")
    AtlasWebElement inexactMatch();

    default String getOfferId() {
        String offerHref = link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/offer\\/(\\d+)\\/");
        Matcher matcher = pattern.matcher(offerHref);
        matcher.find();
        return matcher.group(1);
    }

}
