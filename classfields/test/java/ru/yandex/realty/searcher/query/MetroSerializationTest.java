package ru.yandex.realty.searcher.query;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.offer.ApartmentInfo;
import ru.yandex.realty.model.offer.BuildingInfo;
import ru.yandex.realty.model.offer.CategoryType;
import ru.yandex.realty.model.offer.Money;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.OfferType;
import ru.yandex.realty.model.offer.Transaction;
import ru.yandex.realty.model.serialization.MetroWithDistanceProtoConverter;
import ru.yandex.realty.proto.unified.offer.address.Metro;

import java.util.Arrays;
import java.util.List;

/**
 * author: rmuzhikov
 */
public class MetroSerializationTest extends SearchAndSerializationTest {
    @Test
    public void testMetroSerialization() throws Exception {
        setUp();
        Location location = new Location();
        location.setAccuracy(LocationAccuracy.EXACT);
        location.setGeocoderLocation("Россия, СПБ, ул Матроскина 42", GeoPoint.getPoint(56f, 33f));
        location.setRawAddress("Россия, СПБ, ул Матроскина 42");
        location.setCombinedAddress("Россия, СПБ, ул Матроскина 42");
        location.setGeocoderId(42);
        Metro metroWithDistance1 = MetroWithDistanceProtoConverter.constructNewMessage(1, 1, null, 15);
        Metro metroWithDistance2 = MetroWithDistanceProtoConverter.constructNewMessage(2, 2, null, 15);
        Metro metroWithDistance3 = MetroWithDistanceProtoConverter.constructNewMessage(3, 3, 1, null);
        Metro metroWithDistance4 = MetroWithDistanceProtoConverter.constructNewMessage(4, 4, 1, null);
        location.setMetro(Arrays.asList(metroWithDistance1, metroWithDistance2, metroWithDistance3, metroWithDistance4));

        Transaction transaction = new Transaction();
        transaction.setWholeInRubles(Money.of(Currency.RUR, 2500000));

        Offer offer = new Offer();
        offer.setId(1l);
        offer.setPartnerId(123l);
        offer.setCategoryType(CategoryType.APARTMENT);
        offer.setOfferType(OfferType.SELL);
        offer.setTransaction(transaction);
        offer.setLocation(location);
        offer.createAndGetSaleAgent();
        offer.setBuildingInfo(new BuildingInfo());
        offer.setApartmentInfo(new ApartmentInfo());
        serialize(offer);

        List<Offer> offers = search(new MatchAllDocsQuery());
        System.out.println(offers);
    }
}
