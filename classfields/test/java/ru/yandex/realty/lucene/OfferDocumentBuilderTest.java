package ru.yandex.realty.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexableField;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.extdata.core.lego.Provider;
import ru.yandex.realty.graph.DocumentBuilderHelper;
import ru.yandex.realty.graph.MutableRegionGraph;
import ru.yandex.realty.graph.RegionGraph;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.model.billing.Campaign;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.location.LocationType;
import ru.yandex.realty.model.offer.ApartmentImprovements;
import ru.yandex.realty.model.offer.ApartmentInfo;
import ru.yandex.realty.model.offer.AreaInfo;
import ru.yandex.realty.model.offer.AreaUnit;
import ru.yandex.realty.model.offer.BuildingInfo;
import ru.yandex.realty.model.offer.CommercialInfo;
import ru.yandex.realty.model.offer.CommercialType;
import ru.yandex.realty.model.offer.HouseInfo;
import ru.yandex.realty.model.offer.LotInfo;
import ru.yandex.realty.model.offer.Money;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.PriceInfo;
import ru.yandex.realty.model.offer.PricingPeriod;
import ru.yandex.realty.model.offer.Purpose;
import ru.yandex.realty.model.offer.PurposeWarehouse;
import ru.yandex.realty.model.offer.SaleAgent;
import ru.yandex.realty.model.offer.SalesAgentCategory;
import ru.yandex.realty.model.offer.Transaction;
import ru.yandex.realty.model.offer.TransactionCondition;
import ru.yandex.realty.model.serialization.MetroWithDistanceProtoConverter;
import ru.yandex.realty.model.sites.ExtendedSiteStatistics;
import ru.yandex.realty.picapica.MdsUrlBuilder;
import ru.yandex.realty.proto.unified.offer.address.Highway;
import ru.yandex.realty.proto.unified.offer.address.Station;
import ru.yandex.realty.sites.CampaignService;
import ru.yandex.realty.sites.ExtendedSiteStatisticsStorage;
import ru.yandex.realty.sites.SitesGroupingService;
import ru.yandex.realty.sites.campaign.CampaignStorage;
import ru.yandex.realty.storage.ExpectedMetroStorage;
import ru.yandex.realty.storage.ParkStorage;
import ru.yandex.realty.storage.PondStorage;
import ru.yandex.realty.storage.verba.VerbaStorage;
import ru.yandex.realty.util.MockUtils;
import ru.yandex.realty.util.lucene.DocumentBuilder;
import ru.yandex.realty.util.lucene.DocumentReaderUtils;
import ru.yandex.verba2.model.Dictionary;
import ru.yandex.verba2.model.Term;
import ru.yandex.verba2.model.attribute.Attribute;
import ru.yandex.verba2.model.attribute.StringAttribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;

/**
 * @author aherman
 */
public class OfferDocumentBuilderTest {
    private final GeoPoint GEO_POINT = GeoPoint.getPoint(55.974524f, 37.314098f);
    private final int GEO_ID = 65;
    private final String geocoderAddress = "Россия, Московская область, Солнечногорский район, деревня Черная Грязь, улица Первая, 10";
    private final String rawAddress = "Россия, Московская область, Солнечногорский район, Черная Грязь, улица Первая, 10";

    private static final Map<ApartmentImprovements, Boolean> APARTMENT_IMPROVEMENTS = Cf.newHashMap();
    static {
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.KITCHEN_FURNITURE, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.ROOM_FURNITURE, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.INTERNET, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.PHONE, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.REFRIGERATOR, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.TELEVISION, true);
        APARTMENT_IMPROVEMENTS.put(ApartmentImprovements.WASHING_MACHINE, true);
    }

    private DocumentBuilder<Offer> documentBuilder;

    @Before
    public void setup() throws Exception {
        MutableRegionGraph regionGraph = MutableRegionGraph.createEmptyRegionGraphWithAllFeatures();
        Term term = new Term(999L, "65", "Новосибирск", 1, "test", DateTime.now(), DateTime.now());
        Term newTerm = new Term(term, Cf.list((Attribute) new StringAttribute("city_id", Arrays.asList("65")),
                new StringAttribute("region_id", Arrays.asList("225"))), Collections.<Dictionary>emptyList());

        Node root = new Node();
        root.setGeoId(10000);
        Name name = new Name();
        name.setAddress("RootRegion");
        name.setDisplay("RootRegion");
        root.setName(name);
        root.setType("country");

        regionGraph.addNode(root);
        regionGraph.setRoot(root);

        Node child = new Node();
        child.setGeoId(1);
        name = new Name();
        name.setAddress("Region");
        name.setDisplay("Region");
        child.setName(name);
        child.setType("city");

        Node russia = new Node();
        russia.setGeoId(225);
        name = new Name();
        name.setDisplay("Russia");
        name.setAddress("Russia");
        russia.setName(name);
        russia.setType("country");

        Node novosibObl = new Node();
        novosibObl.setGeoId(113156);
        name = new Name();
        name.setDisplay("Novosibirkaya oblast'");
        name.setAddress("Novosibirkaya oblast'");
        novosibObl.setName(name);
        novosibObl.setGeoObjectType(GeoObjectType.SUBJECT_FEDERATION);

        Node novosib = new Node();
        novosib.setGeoId(65);
        name = new Name();
        name.setDisplay("Novosib");
        name.setAddress("Novosib");
        novosib.setName(name);
        novosib.setType("city");

        regionGraph.addNode(child);
        regionGraph.addNode(russia);
        regionGraph.addNode(novosibObl);
        regionGraph.addNode(novosib);
        child.addParentId(root.getId());
        root.addChildrenId(child.getId());
        root.addChildrenId(russia.getId());
        russia.addParentId(root.getId());
        russia.addChildrenId(novosibObl.getId());
        novosibObl.addParentId(russia.getId());
        novosibObl.addChildrenId(novosib.getId());
        novosib.addParentId(novosibObl.getId());

        Provider<RegionGraph> regionGraphProvider = Mockito.mock(Provider.class);
        Mockito.when(regionGraphProvider.get()).thenReturn(regionGraph);
        Provider<VerbaStorage> verba2Provider = Mockito.mock(Provider.class);
        Mockito.when(verba2Provider.get()).thenReturn(new VerbaStorage(Cf.<Dictionary>newArrayList()));

        SitesGroupingService sitesGroupingService = Mockito.mock(SitesGroupingService.class);
        Mockito.when(sitesGroupingService.getSiteById(anyLong())).thenReturn(null);

        Provider<ExtendedSiteStatisticsStorage> extendedSiteStatisticsStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(extendedSiteStatisticsStorageProvider.get()).thenReturn(new ExtendedSiteStatisticsStorage(Collections.<Long, ExtendedSiteStatistics>emptyMap()));

        Provider<CampaignStorage> campaignStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(campaignStorageProvider.get()).thenReturn(new CampaignStorage(Collections.<Campaign>emptyList()));

        Provider<ExpectedMetroStorage> expectedMetroStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(expectedMetroStorageProvider.get()).thenReturn(ExpectedMetroStorage.empty());

        Provider<PondStorage> pondStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(pondStorageProvider.get()).thenReturn(PondStorage.empty());

        Provider<ParkStorage> parkStorageProvider = Mockito.mock(Provider.class);
        Mockito.when(parkStorageProvider.get()).thenReturn(ParkStorage.empty());

        DocumentBuilderHelper helper = new DocumentBuilderHelper(regionGraphProvider);
        ProtoLuceneDocumentBuilder protoLuceneDocumentBuilder = new ProtoLuceneDocumentBuilder(
                regionGraphProvider,
                helper,
                sitesGroupingService,
                extendedSiteStatisticsStorageProvider,
                new CampaignService(campaignStorageProvider),
                pondStorageProvider,
                parkStorageProvider,
                new MdsUrlBuilder("//avatarnica.test")
        );
        documentBuilder = new ProtoLuceneOfferDocumentBuilder(protoLuceneDocumentBuilder);
    }

    private Offer createFakeOffer() throws Exception {
        Offer offer = new Offer();
        MockUtils.setMockData(offer);

        LotInfo lotInfo = new LotInfo();
        offer.setLotInfo(lotInfo);
        MockUtils.setMockData(lotInfo);
        lotInfo.setLotArea(AreaInfo.create(AreaUnit.HECTARE, 5f));

        HouseInfo houseInfo = new HouseInfo();
        offer.setHouseInfo(houseInfo);
        MockUtils.setMockData(houseInfo);
        houseInfo.setKitchenSpace(10f);
        houseInfo.setLivingSpace(20f);

        BuildingInfo buildingInfo = new BuildingInfo();
        offer.setBuildingInfo(buildingInfo);
        MockUtils.setMockData(buildingInfo);

        ApartmentInfo apartmentInfo = new ApartmentInfo();
        offer.setApartmentInfo(apartmentInfo);
        MockUtils.setMockData(apartmentInfo);
        apartmentInfo.setFloors(Cf.list(1, 2, 3));
        apartmentInfo.setApartmentImprovements(APARTMENT_IMPROVEMENTS);

        offer.setArea(AreaInfo.create(AreaUnit.SQUARE_METER, MockUtils.getValue(Float.class)));

        Transaction transaction = new Transaction();
        offer.setTransaction(transaction);

        final Map<TransactionCondition, Boolean> tc = Cf.newHashMap();
        tc.put(TransactionCondition.MORTGAGE, true);
        transaction.setTransactionConditions(tc);

        transaction.setAreaPrice(PriceInfo.create(Currency.RUR, 123, PricingPeriod.PER_YEAR, AreaUnit.ARE), null);
        Money whole = Money.of(Currency.RUR, 100);
        transaction.setWholeInRubles(whole);

        final SaleAgent agent = offer.createAndGetSaleAgent();
        agent.setCategory(SalesAgentCategory.OWNER);

        createLocation(offer);
        offer.setClusterId(offer.getLongId());
        offer.setClusterHeader(true);
        offer.setClusterSize(1);

        CommercialInfo commercialInfo = new CommercialInfo();
        offer.setCommercialInfo(commercialInfo);
        MockUtils.setMockData(commercialInfo);
        commercialInfo.setCommercialType(CommercialType.AUTO_REPAIR);
        commercialInfo.addCommercialTypeInt(CommercialType.BUSINESS.value());
        commercialInfo.setPurpose(Purpose.BANK);
        commercialInfo.addPurposeInt(Purpose.BEAUTY_SHOP.value());
        commercialInfo.setPurposeWarehouse(PurposeWarehouse.ALCOHOL);
        commercialInfo.addPurposeWarehousesInt(PurposeWarehouse.PHARMACEUTICAL_STOREHOUSE.value());

        return offer;
    }

    private void createLocation(Offer offer) {
        Location location = new Location();
        offer.setLocation(location);
        location.setAccuracy(LocationAccuracy.EXACT);
        location.setType(LocationType.EXACT_ADDRESS);
        location.setHighwayAndDistance(Highway.newBuilder().setId(3).setDistance(10).build());
        location.setGeocoderLocation(geocoderAddress, GEO_POINT);
        location.setGeocoderId(GEO_ID);
        location.setHouseNum("10");
        location.setLocalityName("деревня Черная Грязь");
        location.setManualPoint(GEO_POINT);
        location.setPlace("У леса");
        location.setRawAddress(rawAddress);
        location.setStreet("улица Первая");

        location.setStation(Cf.list(
                Station.newBuilder()
                        .setEsr(60406)
                        .setDistance(20)
                        .build()
        ));

        location.setMetro(Cf.list(MetroWithDistanceProtoConverter.constructNewMessage(1227740l, 0, 0, 30)));

        location.setDistricts(Collections.singletonList(1210971l));
    }

    @Test
    public void testDocumentSerializer() throws Exception {
        Offer fakeOffer = createFakeOffer();

        Document document = documentBuilder.serialize(fakeOffer);
        Document noDocValues = new Document();
        for (IndexableField field : document.getFields()) {
            if (field.fieldType().docValuesType() == DocValuesType.NONE) {
                noDocValues.add(field);
            }
        }
        Offer offer = OfferDocumentDeserializer.toOffer(noDocValues);

        Set<Integer> regions = new HashSet<>();
        regions.addAll(DocumentReaderUtils.integerListValue(noDocValues, OfferDocumentFields.REGION));

        assertTrue(regions.contains(65));
        assertTrue(regions.contains(225));
        assertEquals(fakeOffer.getLongId(), offer.getLongId());
        assertEquals(fakeOffer.getOfferType(), offer.getOfferType());
        assertEquals(fakeOffer.getCategoryType(), offer.getCategoryType());
        assertEquals(fakeOffer.getSupplies(), offer.getSupplies());
        assertEquals(fakeOffer.getLotInfo(), offer.getLotInfo());

        assertNotNull(offer.getApartmentInfo());
        HouseInfo houseInfo = offer.getHouseInfo();
        assertNotNull(houseInfo);
        assertEquals(10f, houseInfo.getKitchenSpace(), 0.001);
        assertEquals(20f, houseInfo.getLivingSpace(), 0.001);
        assertNotNull(offer.getBuildingInfo());
        Transaction transactionInfo = offer.getTransaction();
        assertNotNull(transactionInfo);
        assertTrue(transactionInfo.getTransactionConditions().containsKey(TransactionCondition.MORTGAGE));
        assertTrue(transactionInfo.getTransactionConditions().get(TransactionCondition.MORTGAGE));
        assertNotNull(transactionInfo.getPrice());
        assertEquals(100.0f, transactionInfo.getWholeInRubles().getValue(), 0.001);
        assertEquals(PricingPeriod.PER_YEAR, transactionInfo.getPrice().getPeriod());
        assertEquals(AreaUnit.ARE, transactionInfo.getPrice().getUnit());

        Location location = offer.getLocation();
        assertNotNull(location);
        assertEquals(GEO_POINT, location.getExactPoint());
        assertNotNull(location.getMetro());
        assertEquals(1, location.getMetro().size());
        assertEquals(LocationAccuracy.EXACT, location.getAccuracy());
        assertEquals(LocationType.EXACT_ADDRESS, location.getType());
        assertEquals(rawAddress, location.getRawAddress());
        assertEquals(geocoderAddress, location.getGeocoderAddress());

        assertEquals(fakeOffer.getArea(), offer.getArea());
        assertEquals(APARTMENT_IMPROVEMENTS, offer.getApartmentInfo().getApartmentImprovements());

        CommercialInfo commercialInfo = offer.getCommercialInfo();
        assertNotNull(commercialInfo);
        assertTrue(commercialInfo.getCommercialType().contains(CommercialType.AUTO_REPAIR));
        assertTrue(commercialInfo.getCommercialType().contains(CommercialType.BUSINESS));
        assertTrue(commercialInfo.getPurposes().contains(Purpose.BANK));
        assertTrue(commercialInfo.getPurposes().contains(Purpose.BEAUTY_SHOP));
        assertTrue(commercialInfo.getPurposeWarehouses().contains(PurposeWarehouse.ALCOHOL));
        assertTrue(commercialInfo.getPurposeWarehouses().contains(PurposeWarehouse.PHARMACEUTICAL_STOREHOUSE));
    }
}
