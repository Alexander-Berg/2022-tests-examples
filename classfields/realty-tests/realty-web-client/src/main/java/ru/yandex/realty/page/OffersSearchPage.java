package ru.yandex.realty.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.ShowPhonePopup;
import ru.yandex.realty.element.SubscriptionForm;
import ru.yandex.realty.element.saleads.ListingPresets;
import ru.yandex.realty.element.saleads.NewListingOffer;
import ru.yandex.realty.element.saleads.WithApartmentFilters;
import ru.yandex.realty.element.saleads.WithShowMoreLink;
import ru.yandex.realty.element.saleads.popup.SubscriptionPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created by kopitsa on 27.06.17.
 */
public interface OffersSearchPage extends BasePage, WithApartmentFilters, WithShowMoreLink {

    String SHOW_PHONE = "Показать телефон";

    @Name("Список результатов")
    @FindBy("//li[contains(@class, 'OffersSerp__list-item_type_offer')]")
    ElementsCollection<NewListingOffer> offersList();

    @Name("Список рекламных мест встроенных в выдачу")
    @FindBy("//li[contains(@class,'OffersSerpAd_adType_guadeloupe')]")
    ElementsCollection<Link> adList();

    @Name("Боковое рекламное место «R1»")
    @FindBy("//div[contains(@id,'offers-serp-aside-ad')]")
    AtlasWebElement r1Banner();

    @Name("Рекламное место «С1»")
    @FindBy("//div[contains(@id,'offers-serp-promo-ad')]")
    AtlasWebElement c1Banner();

    @Name("Рекламное место «С2»")
    @FindBy("//div[contains(@id,'offers-serp-inner-ad')]")
    AtlasWebElement c2Banner();

    @Name("Рекламное место «С3»")
    @FindBy("//div[contains(@id,'offers-serp-bottom-ad')]")
    AtlasWebElement c3Banner();

    @Name("Попап «Показать телефон»")
    @FindBy("//div[contains(@class,'Modal_visible PhoneModal')]")
    ShowPhonePopup showPhonePopup();

    @Name("Форма подписки")
    @FindBy("//form[contains(@class,'SearchSubscriptionSerpForm')]")
    SubscriptionForm subscriptionForm();

    @Name("Форма подписки - успешное подписание")
    @FindBy("//div[contains(@class,'SearchSubscriptionSerpForm__containerSuccess')]")
    Link subscriptionSuccess();

    @Name("Пустая выдача")
    @FindBy("//div[contains(@class,'OffersSerp_empty')]")
    Button emptySerp();

    @Name("Попап подписки внизу")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    SubscriptionPopup subscriptionPopup();

    @Name("Попап подписки на цену оффера при наведении на стрелочку")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    SubscriptionPopup priceSubscriptionPopup();

    @Name("Закрытые пресеты")
    @FindBy(".//div[@class='OffersSerpSuggestionPresets']")
    ListingPresets closedPresets();

    @Name("Закрытые пресеты")
    @FindBy(".//div[@class='OffersSerpSuggestionPresets OffersSerpSuggestionPresets_open']")
    ListingPresets openedPresets();

    @Name("Кнопка группы пресетов «{{ value }}»")
    @FindBy(".//div[contains(@class, 'OffersSerpSuggestionPresets__navigation_show')]")
    Button presetGroups();

    @Name("Селектор сортировки")
    @FindBy("//button[contains(@class,'Select__button')]")
    AtlasWebElement sortSelect();

    @Name("Опция «{{ value }}»")
    @FindBy("//div[contains(@class, 'Popup_visible')]" +
            "//div[contains(@class, 'OffersSerpSortSelect__item') and contains(., '{{ value }}')]")
    AtlasWebElement option(@Param("value") String value);

    @Step("Ищем оффер {offerId} в листинге")
    default NewListingOffer findOffer(String offerId) {
        return offersList().stream()
                .filter(offer -> offer.offerLink().getAttribute("href").contains(offerId)).findFirst().get();
    }

    default NewListingOffer offer(int i) {
        return offersList().should(hasSize(greaterThan(i))).get(i);
    }
}
