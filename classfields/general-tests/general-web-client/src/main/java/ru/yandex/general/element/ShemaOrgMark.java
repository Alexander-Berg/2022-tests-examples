package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

public interface ShemaOrgMark extends VertisElement {

    String DATE_POSTED = "datePosted";
    String DESCRIPTION = "description";
    String TITLE = "title";
    String NAME = "name";
    String STREET_ADDRESS = "streetAddress";
    String ADDRESS_LOCALITY = "addressLocality";
    String VALUE = "value";
    String LOW_PRICE = "lowPrice";
    String HIGH_PRICE = "highPrice";
    String OFFER_COUNT = "offerCount";
    String PRICE_CURRENCY = "priceCurrency";
    String OFFERS = "offers";
    String IMAGE = "image";
    String AVAILABILITY = "availability";
    String PRICE = "price";
    String ITEM_CONDITION = "itemCondition";
    String URL = "url";

    @Name("Мета данные «{{ value }}»")
    @FindBy(".//*[@itemprop = '{{ value }}']")
    Meta meta(@Param("value") String value);

    @Name("Мета данные «{{ value }}» списком")
    @FindBy(".//*[@itemprop = '{{ value }}']")
    ElementsCollection<Meta> metaList(@Param("value") String value);

    @Name("Блок разметки «Offer»")
    @FindBy(".//div[@itemtype = 'http://schema.org/Offer']")
    ShemaOrgMark offer();

    @Name("Блок разметки «Brand»")
    @FindBy(".//div[@itemtype = 'http://schema.org/Brand']")
    ShemaOrgMark brand();

    @Name("Блок разметки «Organization»")
    @FindBy(".//div[@itemtype = 'http://schema.org/Organization']")
    ShemaOrgMark organization();

    @Name("Блок разметки «Place»")
    @FindBy(".//div[@itemtype = 'http://schema.org/Place']")
    ShemaOrgMark place();

    @Name("Блок разметки «MonetaryAmount»")
    @FindBy(".//div[@itemtype = 'http://schema.org/MonetaryAmount']")
    ShemaOrgMark monetaryAmount();

    @Name("Блок разметки «QuantitativeValue»")
    @FindBy(".//div[@itemtype = 'http://schema.org/QuantitativeValue']")
    ShemaOrgMark quantitativeValue();

    @Name("Блок разметки «AggregateOffer»")
    @FindBy(".//div[@itemtype = 'http://schema.org/AggregateOffer']")
    ShemaOrgMark aggregateOffer();

}
