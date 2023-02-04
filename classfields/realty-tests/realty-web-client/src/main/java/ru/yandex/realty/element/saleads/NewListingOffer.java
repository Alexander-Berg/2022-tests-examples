package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public interface NewListingOffer extends Button, InputField, RealtyElement, ButtonWithTitle, Link {

    @Name("Список действий над объявлением")
    @FindBy(".//div[contains(@class, 'OffersSerpItem__actions')]")
    ActionBar actionBar();

    @Name("Ссылка на открытие оффера")
    @FindBy(".//div[contains(@class, 'OffersSerpItem__generalInfo')]//a[contains(@class,'OffersSerpItem__link')]")
    AtlasWebElement offerLink();

    @Name("Цена на оффер")
    @FindBy(".//div[contains(@class, 'OffersSerpItem__price')]")
    AtlasWebElement price();

    @Name("Стрелочка возле цены")
    @FindBy("//i[contains(@class,'PriceTrendIndicator')]")
    RealtyElement arrowPrice();

    @Name("Поле ввода заметки")
    @FindBy(".//span[contains(@class, 'OffersSerpItem__note-input')]")
    AddNoteField addNoteField();

    @Name("Сохранить заметку")
    @FindBy(".//button[contains(@class, 'OffersSerpItem__note-save')]")
    AtlasWebElement saveNote();

    @Name("Удалить заметку")
    @FindBy(".//button[contains(@class, 'OffersSerpItem__note-remove')]")
    AtlasWebElement deleteNote();

    @Name("Ссылка на ЖК или КП ")
    @FindBy(".//div[contains(@class,'OffersSerpItem__building')]/a")
    AtlasWebElement residentialLink();

    @Name("Сообщение о что оффер продан")
    @FindBy("//div[@class='RevokedButtonLikeLabel']")
    AtlasWebElement soldMessage();

    @Name("«Позвонить» в карточке коттеджного поселка")
    @FindBy(".//button[contains(@class,'FavoriteVillageSerpItem__phone')]")
    AtlasWebElement showPhoneVillageButton();

    default String getOfferId() {
        String offerHref = link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/offer\\/(\\d+)\\/");
        Matcher matcher = pattern.matcher(offerHref);
        matcher.find();
        return matcher.group(1);
    }
}
