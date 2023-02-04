package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.listing.TouchOffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public interface AmpSaleAdsPage extends SaleAdsPage {

    @Name("amp. Список офферов")
    @FindBy("//div[contains(@class,'OffersSearchPage__list')]/div[contains(@class,'OfferSnippet')]")
    ElementsCollection<TouchOffer> ampOffersList();

    @Name("amp. Следующаяя страница")
    @FindBy(".//amp-next-page/div[@class='i-amphtml-next-page-document-container']")
    ElementsCollection<AtlasWebElement> ampNextPages();

    @Name("amp. Селектор сортировки")
    @FindBy("//select[@on = 'change:AMP.navigateTo(url=event.value)']")
    AtlasWebElement ampSortSelect();

    @Name("amp. Опция сортировки «{{ value }}»")
    @FindBy("//select[@on = 'change:AMP.navigateTo(url=event.value)']/option[contains(., '{{ value }}')]")
    AtlasWebElement ampSortOption(@Param("value") String value);

    @Name("amp. Сниппет ЖК при поиске квартир в ЖК")
    @FindBy(".//div[contains(@class,'SiteSnippet__wrapper')]")
    AtlasWebElement ampSiteSnippet();

    @Name("Элемент который показывает что фильтр отработал и нет 400 или 500")
    @FindBy(".//div[contains(@class,'OffersSearchPage__content')]")
    AtlasWebElement content();


    default TouchOffer ampOffer(int i) {
        return ampOffersList().should(hasSize(greaterThan(i))).get(i);
    }

    default String getAmpOfferId(int i) {
        return getOfferId(ampOffersList().should(hasSize(greaterThan(i))).get(i));
    }

    default String getOfferId(TouchOffer element) {
        String offerHref = element.link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/offer\\/(\\d+)\\/");
        Matcher matcher = pattern.matcher(offerHref);
        matcher.find();
        return matcher.group(1);
    }

    default TouchOffer ampPredLastOffer() {
        int lastOfferIndex = ampOffersList().size() - 2;
        return ampOffersList().get(lastOfferIndex);
    }
}
