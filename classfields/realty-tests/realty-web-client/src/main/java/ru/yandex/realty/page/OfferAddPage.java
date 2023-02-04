package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.PopupWithItem;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.offers.FeatureField;
import ru.yandex.realty.element.offers.LocationControls;
import ru.yandex.realty.element.offers.PriceField;
import ru.yandex.realty.element.offers.PublishBlock;
import ru.yandex.realty.element.offers.auth.ContactInfo;
import ru.yandex.realty.element.offers.rows.PhoneField;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;

/**
 * Created by ivanvan on 17.07.17.
 */
public interface OfferAddPage extends WebPage, SelectionBlock, Button, InputField, Link {

    String NORMAL_SALE = "Обычная продажа";
    String FASTER_SALE = "Продать быстрее";
    String NORMAL_PLACEMENT = "Обычное размещение";
    String PROMOTION = "Продвижение";
    String RISING = "Поднятие";
    String PREMIUM = "Премиум";
    String ADD_PHOTO = "Добавить фотографии";
    String SAVE_CHANGES = "Сохранить изменения";
    String SAVE_FREE = "Сохранить бесплатно";
    String PUBLISH_WITHOUT_PHOTO = "Опубликовать без фото";
    String SAVE_WITHOUT_PHOTO = "Сохранить без фото";
    String PUBLISH_FREE = "Опубликовать бесплатно";
    String PUBLISH_FOR = "Опубликовать за .* ₽";
    String ACTIVATE_AND_PROMOTE = "Активировать и продвинуть за .* ₽";
    String ACTIVATE_FOR = "Активировать за .* ₽";
    String ACTIVATE_FOR_WITHOUT_PHOTO = "Активировать за .* ₽ без фото";
    String ACTIVATE_FREE = "Активировать бесплатно";
    String ACTIVATE_WITHOUT_PHOTO_FREE = "Активировать без фото бесплатно";
    String PLACEMENT_AND_PAY_FOR = "Разместить и оплатить за .* ₽";
    String CONTINUE_WITH_OPTIONS_FOR = "Продолжить с опциями за .* ₽";
    String CONTINUE_FREE = "Продолжить бесплатно";
    String SAVE_AND_PAY = "Сохранить и оплатить .* ₽";
    String SAVE_AND_CONTINUE = "Сохранить и продолжить";
    String PUBLISH_WITH_OPTIONS_FOR = "Опубликовать с опциями за .* ₽";
    String ACTIVATE_WITH_OPTIONS_FOR = "Активировать с опциями за .* ₽";
    String TURBO = "Пакет «Турбо»";
    String FILL_ADDRESS = "Указать адрес объекта";
    String LOGIN = "Войти";
    String REGISTER = "Зарегистрироваться";
    String CONFIRM = "Подтвердить";
    String SAVE_AND_PUBLISH = "Сохранить и оплатить";
    String FLAT = "Квартира";

    @Name("Блок со свойством «{{ value }}»")
    @FindBy("//div[contains(@class, 'offer-form-row') and " +
            "(.//div[contains(@class, 'offer-form-row__title')]/text()='{{ value }}')]")
    FeatureField featureField(@Param("value") String value);

    @Name("Типа сделки")
    @FindBy("//div[@id='form-group-type']")
    SelectionBlock dealType();

    @Name("Блок типа недвижимости")
    @FindBy("//div[@id='form-group-category']")
    SelectionBlock offerType();

    @Name("Блок Адреса")
    @FindBy("//div[@id='form-group-location']")
    LocationControls locationControls();

    @Name("Блок условий сделки")
    @FindBy("//div[@id='form-group-price']")
    PriceField priceField();

    @Name("Поле «Телефон»")
    @FindBy("//div[contains(@class, 'offer-form-row_name_phones')]")
    PhoneField phoneField();

    @Name("Блок логина сверху")
    @FindBy("//div[contains(@class, 'NavBarUserMenu')]")
    Link noAuthTopBlock();

    @Name("Блок логина снизу")
    @FindBy("//div[@class='unauthorized-form']")
    Link noAuthLowerBlock();

    @Name("Попап")
    @FindBy("//div[contains(@class, 'visible_yes') or contains(@class, 'Popup_visible')]")
    PopupWithItem popupWithSpan();

    @Name("Ссылка «Настроить XML-загрузку»")
    @FindBy(".//a[contains(@class, 'add-feed')]")
    AtlasWebElement adjustXML();

    @Name("Блок контактной информации")
    @FindBy("//div[contains(@id, 'form-group-contacts')]")
    ContactInfo contactInfo();

    @Name("Заблокированный кабинет")
    @FindBy("//div[@class='error-message__content']")
    AtlasWebElement errorField();

    @Name("Ошибка адреса")
    @FindBy("//div[@class='offer-field-error']")
    AtlasWebElement addressError();

    @Name("Добавить фото")
    @FindBy("//div[@class='attach']")
    InputField addPhoto();

    @Name("Галерея")
    @FindBy("//div[@id='form-group-photo']")
    EditGallery gallery();

    @Name("Кнопка «Продолжить»")
    @FindBy("//button[contains(@class,'publish-payselector__save_type_edit')]")
    RealtyElement continueButtonOnTrapPage();

    @Name("Карта")
    @FindBy("//div[@class='offer-form__location-map']")
    AtlasWebElement map();

    @Name("Блок публикации")
    @FindBy("//div[@id='form-group-publish']")
    PublishBlock publishBlock();

    @Name("Ловушка публикации")
    @FindBy("//div[contains(@class,'TrapPanel__container')]")
    PublishBlock publishTrap();

    @Name("Блок «mos.ru»")
    @FindBy(".//div[contains(@class,'MosRuSection__container')]")
    AtlasWebElement mosruBLock();
}