package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.AlfaBankMortgage;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.mobile.element.Footer;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.listing.SubscriptionForm;
import ru.yandex.realty.mobile.element.listing.TouchOffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * Created by kopitsa on 16.08.17.
 */
public interface SaleAdsPage extends BasePage, Link, Footer, AlfaBankMortgage {

    String MAKE_CALL = "Позвонить";
    String PARAMETERS = "Параметры";
    String MAP = "Карта";
    int PIXELS_TO_FLOAT_NAV_BAR = 46;
    String SHOW_PREV = "Показать предыдущие";
    String SHOW_NEXT = "Показать ещё";

    @Name("Кнопка выбора фильтров")
    @FindBy("//div[contains(@class,'NavigationBarParamsButton__paramsButton')]")
    AtlasWebElement showFiltersButton();

    @Name("Список офферов")
    @FindBy("//li[contains(@class,'SerpListItem_type_offer')]")
    ElementsCollection<TouchOffer> offersList();

    @Name("Листинг офферов")
    @FindBy("//ol[contains(@class,'OffersSerp__list')]")
    AtlasWebElement listing();

    @Name("Счетчик офферов")
    @FindBy("//div[contains(@class,'SearchResultsCounter')]")
    AtlasWebElement resultCounter();

    @Name("Список пресетов")
    @FindBy("//div[contains(@class, 'Presets__list')]")
    Link presets();

    @Name("Селектор сортировки")
    @FindBy("//nav[contains(@class,'SearchResultsControls')]//button")
    AtlasWebElement sortSelect();

    @Name("Опция сортировки «{{ value }}»")
    @FindBy("//select[@name = 'serp_sort_select']/option[contains(., '{{ value }}')]")
    AtlasWebElement sortOption(@Param("value") String value);

    @Name("Промо в листинге")
    @FindBy("//li[contains(@class, 'OffersSerpPromoItem')]")
    RealtyElement promo();

    @Name("Форма подписки")
    @FindBy("//form[contains(@class, 'SubscriptionForm')]")
    SubscriptionForm subscriptionForm();

    @Name("Хлебная крошка «{{ value }}»")
    @FindBy(".//li[contains(@class,'BreadcrumbsNew__item')]//a[.='{{ value }}']")
    AtlasWebElement breadCrumb(@Param("value") String value);

    default TouchOffer offer(int i) {
        return offersList().should(hasSize(greaterThan(i))).get(i);
    }


    default String getOfferId(int i) {
        return getOfferId(offer(i));
    }


    default String getOfferId(TouchOffer element) {
        String offerHref = element.link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/offer\\/(\\d+)\\/");
        Matcher matcher = pattern.matcher(offerHref);
        matcher.find();
        return matcher.group(1);
    }

    default TouchOffer lastOffer() {
        int lastOfferIndex = offersList().size() - 1;
        return offersList().get(lastOfferIndex);
    }
}
