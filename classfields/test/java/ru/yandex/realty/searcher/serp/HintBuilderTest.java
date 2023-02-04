package ru.yandex.realty.searcher.serp;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.collections.Bag;
import ru.yandex.extdata.core.lego.Provider;
import ru.yandex.realty.context.ExtDataRegionDocumentsStatisticsProvider;
import ru.yandex.realty.context.ProviderAdapter;
import ru.yandex.realty.graph.RegionGraph;
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter;
import ru.yandex.realty.index.DocumentsStatistics;
import ru.yandex.realty.index.OfferFlag;
import ru.yandex.realty.index.RegionDocumentsStatistics;
import ru.yandex.realty.model.offer.CategoryType;
import ru.yandex.realty.model.offer.OfferType;
import ru.yandex.realty.model.offer.RentTime;
import ru.yandex.realty.search.common.request.domain.SearchQuery;
import ru.yandex.realty.searcher.response.Hint;
import ru.yandex.realty.storage.RegionDocumentsStatisticsStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nstaroverova
 */
public class HintBuilderTest {

    private static final long EXISTING_RG_ID = 213;
    private static final long NOT_EXISTING_RG_ID = 214;
    private HintBuilder hintBuilder;

    @Before
    public void setUp() throws Exception {
        RegionGraph regionGraph =
                RegionGraphProtoConverter.deserialize(IOUtils.gunzip(getClass().getClassLoader().getResourceAsStream("region_graph-8-2.data")));
        Provider<RegionDocumentsStatisticsStorage> regionDocumentsStatisticsProvider = mock(ExtDataRegionDocumentsStatisticsProvider.class);
        RegionDocumentsStatistics regionStat = new RegionDocumentsStatistics(EXISTING_RG_ID, generateMskStat());
        RegionDocumentsStatisticsStorage storage = new RegionDocumentsStatisticsStorage(Collections.singletonList(regionStat));
        when(regionDocumentsStatisticsProvider.get()).thenReturn(storage);
        hintBuilder = new HintBuilder(ProviderAdapter.create(regionGraph), regionDocumentsStatisticsProvider);
    }

    @Test
    public void testHintIdNullIfRegionWasNotFound() {
        Hint result = hintBuilder.build(NOT_EXISTING_RG_ID, new SearchQuery());

        assertNull(result);
    }

    @Test
    public void testHintReturnTrue() {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setCategory(CategoryType.APARTMENT);
        searchQuery.setType(OfferType.RENT);
        searchQuery.setMetroGeoId(1);
        Hint result = hintBuilder.build(EXISTING_RG_ID, searchQuery);

        assertNotNull(result);
        assertTrue(result.isNearSearch());
    }

    @Test
    public void testHintReturnFalse() {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setCategory(CategoryType.APARTMENT);
        searchQuery.setType(OfferType.SELL);
        searchQuery.setNewFlat(true);
        searchQuery.setNearSearch(false);

        Hint result = hintBuilder.build(EXISTING_RG_ID, searchQuery);

        assertNotNull(result);
        assertFalse(result.isNearSearch());
    }

    @Test
    public void testHintIdNullIfStatIsEmpty() {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setCategory(CategoryType.GARAGE);
        searchQuery.setType(OfferType.SELL);

        Hint result = hintBuilder.build(EXISTING_RG_ID, searchQuery);

        assertNull(result);
    }

    private List<DocumentsStatistics> generateMskStat() {
        Bag<OfferFlag> offerFlagsBag = Bag.newHashBag();
        offerFlagsBag.add(OfferFlag.NEW_FLAT_YES);
        offerFlagsBag.add(OfferFlag.NEW_FLAT_YES);
        offerFlagsBag.add(OfferFlag.NEW_FLAT_NO);
        List<DocumentsStatistics> statistics = new ArrayList<>();
        DocumentsStatistics sellApartment = new DocumentsStatistics(
                OfferType.SELL, CategoryType.APARTMENT, RentTime.UNKNOWN,
                3, 100, offerFlagsBag);
        DocumentsStatistics largRentApartment = new DocumentsStatistics(
                OfferType.RENT, CategoryType.APARTMENT, RentTime.LARGE,
                1, 100, Bag.newHashBag());
        DocumentsStatistics shortRentApartment = new DocumentsStatistics(
                OfferType.RENT, CategoryType.APARTMENT, RentTime.SHORT,
                1, 100, Bag.newHashBag());
        DocumentsStatistics sellHouse = new DocumentsStatistics(
                OfferType.SELL, CategoryType.HOUSE, RentTime.UNKNOWN,
                2, 100, Bag.newHashBag());
        statistics.add(sellApartment);
        statistics.add(largRentApartment);
        statistics.add(shortRentApartment);
        return statistics;
    }

}
