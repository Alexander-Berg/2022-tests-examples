package ru.yandex.realty.searcher.query;

import junit.framework.Assert;
import org.apache.lucene.search.Query;
import org.junit.Test;
import ru.yandex.realty.model.offer.ApartmentInfo;
import ru.yandex.realty.model.offer.BuildingInfo;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.search.common.request.domain.SearchQuery;
import ru.yandex.realty.searcher.query.clausebuilder.PrimarySaleClauseBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * User: daedra
 * Date: 18.07.14
 * Time: 16:33
 */
public class PrimarySaleTest extends SearchAndSerializationTest {
    @Test
    public void testPrimarySaleTrue() throws IOException {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setPrimarySale(true);

        Offer offer1 = createOffer(1L, true, true);
        Offer offer2 = createOffer(2L, false, true);
        Offer offer3 = createOffer(3L, true, false);
        Offer offer4 = createOffer(4L, false, false);
        Offer offer5 = createOffer(5L, true, null);
        Offer offer6 = createOffer(6L, false, null);

        Offer[] offers = new Offer[]{offer1, offer2, offer3, offer4, offer5, offer6};

        setUp();
        serialize(offers);

        PrimarySaleClauseBuilder primarySaleClauseBuilder = new PrimarySaleClauseBuilder();
        Query query = primarySaleClauseBuilder.createSubQuery(searchQuery);
        List<Offer> foundOffers = search(query);
        Set<Long> foundOffersIds = newHashSet();
        for(Offer offer : foundOffers) {
            foundOffersIds.add(offer.getLongId());
        }

        Assert.assertTrue("Offer 0 not found", foundOffersIds.contains(offers[0].getLongId()));
        Assert.assertTrue("Offer 1 not found", foundOffersIds.contains(offers[1].getLongId()));
        Assert.assertFalse("Offer 2 found", foundOffersIds.contains(offers[2].getLongId()));
        Assert.assertFalse("Offer 3 found", foundOffersIds.contains(offers[3].getLongId()));
        Assert.assertFalse("Offer 4 found", foundOffersIds.contains(offers[4].getLongId()));
        Assert.assertFalse("Offer 5 found", foundOffersIds.contains(offers[5].getLongId()));
        cleanup();

    }

    @Test
    public void testPrimarySaleFalse() throws IOException {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setPrimarySale(false);

        Offer offer1 = createOffer(1l, true, true);
        Offer offer2 = createOffer(2l, false, true);
        Offer offer3 = createOffer(3l, true, false);
        Offer offer4 = createOffer(4l, false, false);
        Offer offer5 = createOffer(5l, true, null);
        Offer offer6 = createOffer(6l, false, null);

        Offer[] offers = new Offer[]{offer1, offer2, offer3, offer4, offer5, offer6};

        setUp();
        serialize(offers);

        PrimarySaleClauseBuilder primarySaleClauseBuilder = new PrimarySaleClauseBuilder();
        Query query = primarySaleClauseBuilder.createSubQuery(searchQuery);
        List<Offer> foundOffers = search(query);
        Set<Long> foundOffersIds = newHashSet();
        for(Offer offer : foundOffers) {
            foundOffersIds.add(offer.getLongId());
        }

        Assert.assertFalse(foundOffersIds.contains(offers[0].getLongId()));
        Assert.assertFalse(foundOffersIds.contains(offers[1].getLongId()));
        Assert.assertTrue(foundOffersIds.contains(offers[2].getLongId()));
        Assert.assertTrue(foundOffersIds.contains(offers[3].getLongId()));
        Assert.assertTrue(foundOffersIds.contains(offers[4].getLongId()));
        Assert.assertTrue(foundOffersIds.contains(offers[5].getLongId()));

        cleanup();

    }

    @Test
    public void testPrimarySaleUndefined() throws IOException {
        SearchQuery searchQuery = new SearchQuery();

        PrimarySaleClauseBuilder primarySaleClauseBuilder = new PrimarySaleClauseBuilder();
        Query query = primarySaleClauseBuilder.createSubQuery(searchQuery);
        Assert.assertNull(query);

    }

    private Offer createOffer(long id, boolean isClusterHead, Boolean primarySale) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setClusterId(isClusterHead ? id : id + 1000);
        offer.setClusterHeader(isClusterHead);
        if (!isClusterHead) {
            offer.setHeadL(1.0f);
            offer.setHeadR(1.0f);
        }
        offer.createAndGetSaleAgent();
        offer.setBuildingInfo(new BuildingInfo());
        if (primarySale != null) {
            offer.getBuildingInfo().setSiteId(1L);
            offer.setPrimarySaleV2(primarySale);
        } else {
            offer.setPrimarySaleV2(false);
        }
        offer.setApartmentInfo(new ApartmentInfo());
        return offer;
    }

}
