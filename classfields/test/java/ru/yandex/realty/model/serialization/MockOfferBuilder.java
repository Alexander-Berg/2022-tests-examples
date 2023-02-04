package ru.yandex.realty.model.serialization;

import com.google.protobuf.util.Timestamps;
import org.joda.time.DateTime;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.location.LocationType;
import ru.yandex.realty.model.offer.ApartmentImprovements;
import ru.yandex.realty.model.offer.ApartmentInfo;
import ru.yandex.realty.model.offer.AreaInfo;
import ru.yandex.realty.model.offer.AreaUnit;
import ru.yandex.realty.model.offer.BuildingImprovements;
import ru.yandex.realty.model.offer.BuildingInfo;
import ru.yandex.realty.model.offer.CategoryType;
import ru.yandex.realty.model.offer.CommercialBuildingType;
import ru.yandex.realty.model.offer.CommercialInfo;
import ru.yandex.realty.model.offer.CommercialInfoImprovements;
import ru.yandex.realty.model.offer.CommercialType;
import ru.yandex.realty.model.offer.EntranceType;
import ru.yandex.realty.model.offer.HouseInfo;
import ru.yandex.realty.model.offer.LotInfo;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.OfferFlag;
import ru.yandex.realty.model.offer.OfferType;
import ru.yandex.realty.model.offer.ParkingType;
import ru.yandex.realty.model.offer.Purpose;
import ru.yandex.realty.model.offer.PurposeWarehouse;
import ru.yandex.realty.model.offer.Quality;
import ru.yandex.realty.model.offer.Supply;
import ru.yandex.realty.model.offer.TaxationForm;
import ru.yandex.realty.model.offer.Transaction;
import ru.yandex.realty.model.offer.TransactionCondition;
import ru.yandex.realty.model.offer.UserComment;
import ru.yandex.realty.model.offer.WindowType;
import ru.yandex.realty.model.sites.Decoration;
import ru.yandex.realty.model.util.OfferClusterizationUtils;
import ru.yandex.realty.proto.unified.offer.address.Highway;
import ru.yandex.realty.proto.unified.offer.address.Station;
import ru.yandex.realty.proto.unified.offer.images.ImageSemanticType;
import ru.yandex.realty.proto.unified.offer.images.MdsImageId;
import ru.yandex.realty.proto.unified.offer.images.RealtyPhotoInfo;
import ru.yandex.realty.proto.unified.offer.images.UnifiedImages;
import ru.yandex.realty.vos.model.offer.CuratedFlatPlan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author aherman
 */
public class MockOfferBuilder {
    public static Offer createMockOffer() {
        Offer offer = new Offer();
        Location location = new Location();
        offer.createAndGetSaleAgent();
        Transaction transaction = new Transaction();
        ApartmentInfo apartmentInfo = new ApartmentInfo();
        HouseInfo houseInfo = new HouseInfo();
        BuildingInfo buildingInfo = new BuildingInfo();
        LotInfo lotInfo = new LotInfo();
        CommercialInfo commercialInfo = new CommercialInfo();

        offer.setLocation(location);
        offer.setTransaction(transaction);
        offer.setApartmentInfo(apartmentInfo);
        offer.setHouseInfo(houseInfo);
        offer.setBuildingInfo(buildingInfo);
        offer.setLotInfo(lotInfo);
        offer.setCommercialInfo(commercialInfo);

        offer.setOfferType(OfferType.RENT);
        offer.setCategoryType(CategoryType.ROOMS);

        offer.setId(11223344L);
        offer.setClusterInfo(OfferClusterizationUtils.buildClusterInfo(offer));
        offer.setPartnerId(1234);
        offer.setOfferUrl("http://example.com/akme-1");
        offer.setMobileUrl("http://example.com/akme-mobile-1");

        offer.setUserComment(UserComment.GOOD);
        offer.getOfferFlags().add(OfferFlag.MANUALLY_ADDED);
        offer.getOfferFlags().add(OfferFlag.NOT_FOR_AGENTS);
        offer.getOfferFlags().add(OfferFlag.PAYED);

        offer.setCreateTime(new DateTime(2012, 7, 1, 10, 11, 12).toInstant());
        offer.setActualUpdateTime(new DateTime(2012, 7, 10, 11, 12, 20).getMillis());
        offer.setUpdateTime(new DateTime(2012, 7, 10, 11, 12, 20).toInstant());
        offer.setExpireTime(new DateTime(2012, 7, 20, 5, 7, 11).toInstant());

        offer.setQuality(Quality.GOOD);

        offer.setDescription("Long description");
        offer.setPhotos(
                UnifiedImages.newBuilder()
                        .addImage(
                                RealtyPhotoInfo.newBuilder()
                                        .setExternalUrl("http://example.org/akme.jpg")
                                        .setMdsId(
                                                MdsImageId.newBuilder()
                                                        .setKnownNamespace(MdsImageId.KnownNamespace.REALTY)
                                                        .setGroup(1)
                                                        .setName("abc")
                                        )
                                        .setCreated(Timestamps.fromMillis(System.currentTimeMillis()))
                                )
                        .addImage(
                                RealtyPhotoInfo.newBuilder()
                                        .setExternalUrl("http://example.org/plan.jpg")
                                        .setMdsId(
                                                MdsImageId.newBuilder()
                                                        .setKnownNamespace(MdsImageId.KnownNamespace.REALTY)
                                                        .setGroup(1)
                                                        .setName("abc")
                                        )
                                        .setCreated(Timestamps.fromMillis(System.currentTimeMillis()))
                                        .setTag("plan")
                                        .setSemanticType(ImageSemanticType.IST_PLAN)
                                )
                        .build(),
                100
        );

        offer.setArea(AreaInfo.create(AreaUnit.SQUARE_METER, 20F));
        offer.setHasAlarm(true);

        Map<Supply, Boolean> supplies = new HashMap<Supply, Boolean>();
        supplies.put(Supply.ELECTRICITY, true);
        supplies.put(Supply.GAS, true);
        supplies.put(Supply.HEATING, true);
        supplies.put(Supply.SEWERAGE, true);
        supplies.put(Supply.WATER, true);
        offer.setSupplies(supplies);

        location.setGeocoderId(213);
        location.setDistricts(Collections.singletonList(1L));
        location.setMicroDistrict(3l);
        location.setLocalityName("Москва");
        location.setStreet("Новый Арбат");
        location.setHouseNum("19");
        location.setGeocoderLocation("Россия, Москва, улица Новый Арбат, 19", GeoPoint.getPoint(55.751885F, 37.588649F));

        location.setStation(Collections.singletonList(
                Station.newBuilder()
                        .setEsr(1)
                        .setDistance(123)
                        .build()
        ));

        location.setHighwayAndDistance(Highway.newBuilder().setId(12343L).setDistance(234).build());

        location.setMetro(Collections.singletonList(MetroWithDistanceProtoConverter.constructNewMessage(32, 1, 5, 10)));

        location.setRawAddress("Россия, Москва, улица Новый Арбат, 19");
        location.setCombinedAddress("Россия, Москва, улица Новый Арбат, 19");

        location.setHasMap(true);
        location.setAccuracy(LocationAccuracy.EXACT);
        location.setType(LocationType.EXACT_ADDRESS);
        location.setShowOnMap(true);

        commercialInfo.setCommercialType(CommercialType.AUTO_REPAIR);
        commercialInfo.addCommercialTypeInt(CommercialType.BUSINESS.value());
        commercialInfo.setCommercialBuildingType(CommercialBuildingType.RESIDENTIAL_BUILDING);
        commercialInfo.setPurpose(Purpose.BANK);
        commercialInfo.addPurposeInt(Purpose.BEAUTY_SHOP.value());
        commercialInfo.setPurposeWarehouse(PurposeWarehouse.ALCOHOL);
        commercialInfo.addPurposeWarehousesInt(PurposeWarehouse.PHARMACEUTICAL_STOREHOUSE.value());
        commercialInfo.setImprovementsInt((int)CommercialInfoImprovements.FREIGHT_ELEVATOR.mask(), true);
        commercialInfo.setPalletPrice(1000.0f);
        commercialInfo.setTemperatureComment("test comment");

        transaction.setCommission(100);
        transaction.setSecurityPayment(10);
        transaction.setTransactionConditionsInt((int)TransactionCondition.CLEANING_INCLUDED.mask(), true);
        transaction.setTaxationForm(TaxationForm.NDS);

        apartmentInfo.setEntranceType(EntranceType.COMMON);
        apartmentInfo.setPhoneLines(5);
        apartmentInfo.setElectricCapacity(10);
        apartmentInfo.setWindowType(WindowType.DISPLAY);
        apartmentInfo.setApartmentImprovementsInt((int)ApartmentImprovements.ADDING_PHONE_ON_REQUEST.mask(), true);
        apartmentInfo.setDecoration(Decoration.UNKNOWN);
        apartmentInfo.setCuratedFlatPlan(CuratedFlatPlan.newBuilder()
                .setAuto(true)
                .setRemoved(false)
                .setUrl("")
                .build());

        buildingInfo.setBuildingImprovement(BuildingImprovements.EATING_FACILITIES, true, false);
        buildingInfo.setParkingPlaces(5);
        buildingInfo.setParkingPlacePrice(1000);
        buildingInfo.setParkingGuestPlaces(10);
        buildingInfo.setParkingType(ParkingType.NEARBY);

        return offer;
    }
}
