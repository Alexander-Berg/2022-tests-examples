package ru.yandex.realty.model.serialization;

import org.joda.time.Instant;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.realty.model.offer.BuildingState;
import ru.yandex.realty.model.offer.OfficeClass;
import ru.yandex.realty.model.offer.SalesAgentCategory;
import ru.yandex.realty.model.raw.*;
import ru.yandex.realty.vos.model.offer.CuratedFlatPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author aherman
 */
public class MockRawOfferBuilder {
    public static RawOfferImpl createMockRawOfferOld() {
        RawOfferImpl rawOfferExt = new RawOfferImpl();

        rawOfferExt.setInternalId("id-1");
        rawOfferExt.setType("SELL");
        rawOfferExt.setCategory("APARTMENT");

        rawOfferExt.setPayedAdv(true);

        rawOfferExt.setUrl("http://example.com/offer-1");
        rawOfferExt.setCreationDate(new Instant("2012-10-10T12:20:34+04:00"));
        rawOfferExt.setLastUpdateDate(new Instant("2012-10-20T12:20:34+04:00"));
        rawOfferExt.setExpireDate(new Instant("2012-10-30T12:20:34+04:00"));

        RawLocationExt locationExt = new RawLocationExt();
        rawOfferExt.setLocation(locationExt);
        locationExt.setCountry("Russia");
        locationExt.setRegion("Moscow-region");
        locationExt.setDistrict("Moscow-district");
        locationExt.setLocalityName("Moscow-locality");
        locationExt.setSubLocalityName("Moscow-subLocality");
        locationExt.setNonAdminSubLocality("Moscow-nonadm");
        locationExt.setAddress("address");
        locationExt.setDirection("direction");
        locationExt.setDistance(10.0f);
        locationExt.setLatitude(20.0f);
        locationExt.setLongitude(30.0f);

        List<RawMetroOld> metro = locationExt.getMetro();
        RawMetroOld metroOld = new RawMetroOld();
        metro.add(metroOld);
        metroOld.setName("metro");
        metroOld.setTimeOnFoot(11);
        metroOld.setTimeOnTransport(21);

        locationExt.setRailwayStation("station");

        RawSalesAgentOld salesAgentExt = new RawSalesAgentOld();
        rawOfferExt.setSalesAgent(salesAgentExt);
        salesAgentExt.setName("name-3");
        salesAgentExt.setPhone("123-34-56");
        salesAgentExt.setCategory(SalesAgentCategory.AGENCY);
        salesAgentExt.setOrganization("organization");
        salesAgentExt.setAgencyId("agency-id");
        salesAgentExt.setUrl("http://example.com/agency");
        salesAgentExt.setEmail("fakemail");
        salesAgentExt.setPartner("partner");
        salesAgentExt.setIsHidden(true);

        RawPriceOld priceOld = new RawPriceOld();
        rawOfferExt.setPrice(priceOld);
        priceOld.setCurrency("RUR");
        priceOld.setUnit("SQUARE_METER");
        priceOld.setValue(34.0f);

        rawOfferExt.setNotForAgents(true);
        rawOfferExt.setHaggle(true);
        rawOfferExt.setMortgage(true);
        rawOfferExt.setPrepayment(45);
        rawOfferExt.setAgentFee(46.0f);

        RawImageImpl rawImage = new RawImageImpl();
        rawImage.setImage("http://exampl.com/img-1");
        rawOfferExt.setAllImages(Collections.singletonList(rawImage));

        rawOfferExt.setQuality("EXCELLENT");
        rawOfferExt.setRenovation("BEFORE_CLEAN");

        rawOfferExt.setDescription("description");

        RawAreaOld area = new RawAreaOld();
        rawOfferExt.setArea(area);
        area.setUnit("SQUARE_METER");
        area.setValue(123.0f);

        RawAreaOld livingSpace = new RawAreaOld();
        rawOfferExt.setLivingSpace(livingSpace);
        livingSpace.setUnit("ARE");
        livingSpace.setValue(234.0f);

        RawAreaOld kitchen = new RawAreaOld();
        rawOfferExt.setKitchenSpace(kitchen);
        kitchen.setUnit("HECTARE");
        kitchen.setValue(23.0f);
        
        rawOfferExt.setNewFlat(true);
        rawOfferExt.setRooms(200);
        rawOfferExt.setOpenPlan(true);

        rawOfferExt.setRoomsType("ISOLATED");
        rawOfferExt.setPhone(true);
        rawOfferExt.setInternet(true);
        rawOfferExt.setRoomFurniture(true);
        rawOfferExt.setKitchenFurniture(true);
        rawOfferExt.setTelevision(true);
        rawOfferExt.setWashingMachine(true);
        rawOfferExt.setRefrigerator(true);

        rawOfferExt.setBalcony("LOGGIA");
        rawOfferExt.setBathroomUnit("MATCHED");
        rawOfferExt.setFloorCovering("LAMINATED_FLOORING_BOARD");
        rawOfferExt.setWindowView("YARD_STREET");

        rawOfferExt.setFloor(50);

        rawOfferExt.setFloorsTotal(51);
        rawOfferExt.setYandexBuildingId(58307l);
        rawOfferExt.setYandexCommercialBuildingId(86409L);
        rawOfferExt.setYandexCommercialHouseId(987403L);
        rawOfferExt.setBuildingName("building-name");
        rawOfferExt.setBuildingType("MONOLIT_BRICK");
        rawOfferExt.setBuildingSeries("building-series");
        rawOfferExt.setBuildingState(BuildingState.HAND_OVER);
        rawOfferExt.setBuiltYear(2012);
        rawOfferExt.setReadyQuarter(3);

        rawOfferExt.setLift(true);
        rawOfferExt.setRubbishChute(true);

        rawOfferExt.setIsElite(true);
        rawOfferExt.setParking(true);
        rawOfferExt.setAlarm(true);
        rawOfferExt.setCeilingHeight("3.0");

        rawOfferExt.setOfficeClass(OfficeClass.B);
        rawOfferExt.setPassBy(true);

        return rawOfferExt;
    }

    public static RawOfferImpl createMockRawOfferOldWithAgentPhoto() {
        RawOfferImpl rawOfferExt = createMockRawOfferOld();
        rawOfferExt.getSalesAgent().setPhoto("http://testPhoto");
        return rawOfferExt;
    }

    public static RawOfferImpl createMockRawOfferOfficeRentExt(boolean withCuratedFlatPlan) {
        RawOfferImpl r = new RawOfferImpl();
        r.setInternalId("7");
        r.setType("RENT");
        r.setCategory("COMMERCIAL");
        r.setCommercialTypes(CollectionFactory.list("FREE_PURPOSE", "RETAIL"));
        r.setCommercialBuildingType("BUSINESS_CENTER");
        r.setPurposes(CollectionFactory.list("BANK", "FOOD_STORE"));
        r.setPurposeWarehouses(CollectionFactory.list("ALCOHOL", "VEGETABLE_STOREHOUSE"));
        r.setUrl("http://yandex.ru/7/");

        r.setCreationDate(new Instant("2010-12-05T00:00:00+04:00"));
        r.setLastUpdateDate(new Instant("2010-12-12T00:00:00+04:00"));
        r.setExpireDate(new Instant("2011-12-12T00:00:00+04:00"));

        RawLocationExt locationExt = new RawLocationExt();
        r.setLocation(locationExt);
        locationExt.setCountry("Россия");
        locationExt.setLocalityName("Санкт-Петербург");
        locationExt.setSubLocalityName("Адмиралтейский");
        locationExt.setAddress("Измайловский пр., 16");

        List<RawMetroOld> metro =locationExt.getMetro();
        RawMetroOld metroOld = new RawMetroOld();
        metro.add(metroOld);
        metroOld.setName("Технологический институт");
        metroOld.setTimeOnFoot(10);
        metroOld.setTimeOnTransport(5);

        RawSalesAgentOld salesAgentExt = new RawSalesAgentOld();
        r.setSalesAgent(salesAgentExt);
        salesAgentExt.setName("Алексей");
        salesAgentExt.setPhone("8-903-808-00-53");
        salesAgentExt.setCategory(SalesAgentCategory.AGENCY);
        salesAgentExt.setOrganization("Агентство недвижимости РУССКИЙ ДОМ");
        salesAgentExt.setUrl("http://anrd.ru/");
        salesAgentExt.setEmail("anrusdom@ya.ru");

        RawPriceOld priceOld = new RawPriceOld();
        r.setPrice(priceOld);
        priceOld.setCurrency("RUR");
        priceOld.setPeriod("PER_MONTH");
        priceOld.setValue(85000.f);
        priceOld.setTaxationForm("NDS");

        r.setCommission(0);
        r.setSecurityPayment(10);
        r.setCleaningIncluded(true);
        r.setUtilitiesIncluded(true);
        r.setElectricityIncluded(true);
        r.setDealStatus("DIRECT_RENT");

        r.setAllImages(CollectionFactory.list(
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882313.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882363.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882395.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882424.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882615.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882707.jpg"),
                new RawImageImpl("http://yandex.ru/components/com_estateagent/pictures/ea1279882762.jpg")
        ));

        r.setRenovation("DESIGNER_RENOVATION");
        r.setQuality("GOOD");
        r.setDescription("Отличное помещение. Можно использовать как офис или зал для занятий йогой.");
        r.setRooms(2);
        r.setEntranceType("SEPARATE");
        r.setPhoneLines(2);
        r.setAddingPhoneOnRequest(true);
        r.setInternet(true);
        r.setSelfSelectionTelecom(true);
        r.setRoomFurniture(true);
        r.setAirConditioner(true);
        r.setVentilation(true);
        r.setFireAlarm(true);
        r.setHeatingSupply(true);
        r.setWaterSupply(true);
        r.setSewerageSupply(true);
        r.setElectricitySupply(true);
        r.setElectricCapacity(12);
        r.setGasSupply(true);
        r.setFloorCovering("GLAZED");
        r.setWindowView("STREET");
        r.setWindowType("DISPLAY");
        r.setFloor(4);

        r.setResponsibleStorage(true);
        r.setFreightElevator(true);
        r.setTruckEntrance(true);
        r.setRamp(true);
        r.setRailway(true);
        r.setOfficeWarehouse(true);
        r.setOpenArea(true);
        r.setServiceThreePl(true);
        r.setPalletPrice(200.f);
        r.setTemperatureComment("ok");

        r.setFloorsTotal(6);
        r.setBuildingName("БЦ Девяткино");
        r.setOfficeClass(OfficeClass.B_PLUS);
        r.setBuildingType("BRICK");
        r.setBuildingSeries("seria");
        r.setBuiltYear(1999);
        r.setAccessControlSystem(true);
        r.setTwentyFourSeven(true);
        r.setLift(true);
        r.setParking(true);
        r.setParkingPlaces(4);
        r.setParkingPlacePrice(7000.0f);
        r.setParkingGuest(true);
        r.setParkingGuestPlaces(2);
        r.setAlarm(false);
        r.setSecurity(true);
        r.setCeilingHeight("2.5");
        r.setEatingFacilities(true);
        if (withCuratedFlatPlan) {
            r.setCuratedFlatPlan(CuratedFlatPlan.newBuilder()
                    .setUrl("http://some.com")
                    .setAuto(true)
                    .setRemoved(false)
                    .build());
        }
        return r;
    }

    public static RawOfferImpl createRawOfferWithExperimentalFields() {
        RawOfferImpl r = createMockRawOfferOld();
        r.setVerified(true);
        r.setMortgageApprove(1);
        r.setCuratedFlatPlan(CuratedFlatPlan.newBuilder()
                .setUrl("http://some.com")
                .setAuto(true)
                .setRemoved(false).build());
        return r;
    }
}
