package ru.yandex.realty.consts;

import ru.auto.test.api.realty.offer.create.userid.Location;
import ru.yandex.realty.step.OfferBuildingSteps;

import static ru.auto.test.api.realty.OfferType.APARTMENT_RENT;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;

/**
 * @author kurau (Yuri Kalinin)
 */
public class OfferByRegion {

    public static Location getLocationForRegion(Region region) {
        switch (region) {
            case HIGH:
                return getHighRaisingLocation();
            case LOW:
                return getLowRaisingLocation();
            case SUPER_HIGH:
                return getUltraHighRaisingLocation();
            case SECONDARY:
                return getSecondaryLocation();
            case REGION:
                return getRegionLocation();
            case NEW_SECONDARY:
                return getNewSecondaryLocation();
            case NEW_BUILDING_SECONDARY:
                return getNewBuildingLocation();
            case JK_BUILDING:
                return getJkBuildingLocation();
            case EGRN_SUCCESS:
                return getEgrnSuccessLocation();
            case EGRN_FAIL:
                return getEgrnFailLocation();
            default:
                return getHighRaisingLocation();
        }
    }

    private static Location getUltraHighRaisingLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(37.56947).withLatitude(55.659904).withRgid(193332L)
                .withAddress("Москва, улица Цюрупы, 20к1");
    }

    private static Location getHighRaisingLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(53.69276).withLatitude(63.56015).withRgid(381443L)
                .withAddress("Республика Коми, Ухта, парк Культуры и Отдыха");
    }

    private static Location getLowRaisingLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(36.590378).withLatitude(50.59853)
                .withRgid(480207L).withAddress("Белгород, Преображенская улица");
    }

    private static Location getRegionLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(73.31742858886719).withLatitude(54.98674774169922)
                .withRgid(552970L).withAddress("Омск, улица Дмитриева, 5/3");
    }

    private static Location getSecondaryLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(30.423080).withLatitude(59.830102)
                .withRgid(417899L).withAddress("Россия, Санкт-Петербург, Бухарестская улица, 156к1");
    }

    private static Location getNewSecondaryLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(30.440238).withLatitude(59.937887)
                .withRgid(417899L).withAddress("Россия, Санкт-Петербург, проспект Энергетиков, 9к6");
    }

    private static Location getNewBuildingLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(30.309037).withLatitude(59.898824)
                .withRgid(417899L).withAddress("Россия, Санкт-Петербург, Малая Митрофаньевская улица");
    }

    private static Location getJkBuildingLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_RENT).getLocation()
                .withLongitude(30.65831).withLatitude(60.036686)
                .withRgid(417867L).withAddress("Россия, Ленинградская область, Всеволожск, проезд Берёзовая Роща, 3");
    }

    private static Location getEgrnSuccessLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(37.58830261).withLatitude(55.60764694).withCountry("Россия")
                .withRgid(741964L).withAddress("Чертановская улица, 48к2").withApartment("511");
    }

    private static Location getEgrnFailLocation() {
        return OfferBuildingSteps.getDefaultOffer(APARTMENT_SELL).getLocation()
                .withLongitude(37.58830261).withLatitude(55.60764694).withCountry("Россия")
                .withRgid(741964L).withAddress("Чертановская улица, 48к2").withApartment("512");
    }


    public enum Region {
        LOW,
        HIGH,
        SUPER_HIGH,
        SECONDARY,
        REGION,
        NEW_SECONDARY,
        NEW_BUILDING_SECONDARY,
        JK_BUILDING,
        EGRN_SUCCESS,
        EGRN_FAIL,
    }

}
