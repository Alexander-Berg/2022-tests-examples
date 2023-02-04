package ru.yandex.realty.model.sites;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.message.ExtDataSchema;
import ru.yandex.realty.model.offer.BuildingState;
import ru.yandex.realty.model.offer.BuildingType;
import ru.yandex.realty.model.serialization.MetroWithDistanceProtoConverter;
import ru.yandex.realty.model.serialization.SiteProtoConverter;
import ru.yandex.realty.model.sites.special.proposals.Discount;
import ru.yandex.realty.model.sites.special.proposals.Gift;
import ru.yandex.realty.model.sites.special.proposals.Installment;
import ru.yandex.realty.model.sites.special.proposals.Sale;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * User: daedra
 * Date: 21.02.14
 * Time: 20:51
 */
@Ignore
public class SitesProtoConverterTest {

    @Test
    public void testEmpty() {
        Site site = new Site(1);
        site.setName("1");
        site.setLocation(new Location());
        site.setHouseMatchFallbackLocation(new Location());

        ExtDataSchema.SiteMessage msg = SiteProtoConverter.toMessage(site);
        Site converted = SiteProtoConverter.fromMessage(msg);

        Assert.assertEquals(site, converted);
    }

    @Test
    public void testFull() {
        Site site = prepareSiteInitializedFully();
        ExtDataSchema.SiteMessage msg = SiteProtoConverter.toMessage(site);

//        try {
//            msg.writeTo(new FileOutputStream(new File("/tmp/1.bin")));
//            SiteWithPhaseProtoConverter.toMessage(site, site.getPhases().get(0)).writeTo(new FileOutputStream(new File("/tmp/phase.bin")));
//            SiteWithSpecialProposalsProtoConverter.toMessage(site, site.getSales().get(0)).writeTo(new FileOutputStream(new File("/tmp/special.bin")));
//        } catch (Exception f) {
//        }

        Site converted = SiteProtoConverter.fromMessage(msg);

        Assert.assertEquals(site, converted);
    }

    @Test
    public void testFullWithMaternityFundsNullExplicit() {
        Site site = prepareSiteInitializedFully();
        site.setMaternityFunds(null);
        ExtDataSchema.SiteMessage msg = SiteProtoConverter.toMessage(site);
        Site converted = SiteProtoConverter.fromMessage(msg);
        Assert.assertEquals(site, converted);
    }

    @Test
    public void testFullWithMaternityFundsTrue() {
        Site site = prepareSiteInitializedFully();
        site.setMaternityFunds(Boolean.TRUE);
        ExtDataSchema.SiteMessage msg = SiteProtoConverter.toMessage(site);
        Site converted = SiteProtoConverter.fromMessage(msg);
        Assert.assertEquals(site, converted);
    }

    @Test
    public void testFullWithMaternityFundsFalse() {
        Site site = prepareSiteInitializedFully();
        site.setMaternityFunds(Boolean.FALSE);
        ExtDataSchema.SiteMessage msg = SiteProtoConverter.toMessage(site);
        Site converted = SiteProtoConverter.fromMessage(msg);
        Assert.assertEquals(site, converted);
    }

    @NotNull
    private Site prepareSiteInitializedFully() {
        Site site = new Site(1);
        site.setAddress("1");
        site.setApartmentsCount(2);
        Company builder = new Company(3);
        site.setBuilders(Collections.singletonList(5L));
        builder.setLogo("2");
        builder.setName("3");
        Organization organization = new Organization(4);
        builder.setOrganizations(Arrays.asList(organization));
        organization.setInn("4");
        organization.setKpp("5");
        organization.setName("6");
        organization.setOrganizationForm(OrganizationForm.OOO);
        builder.setPhoneNumbers(Arrays.asList("7"));
        builder.setRating(5);
        builder.setStartDate(new Date());
        builder.setUrl("8");
        site.setBuildingClass(BuildingClass.ECONOM);
        site.setBuildingTypes(Collections.singletonList(BuildingType.BRICK));
        site.setDescription("9");
        site.setSeoDescription("99");
        site.setElevatorsBrand("10");
        site.setFullLivingSpace(7);
        site.setGeoPoint(GeoPoint.getPoint(8.1f, 9.1f));
        site.setParkings(
                ExtDataSchema.SiteParkingsMessage
                        .newBuilder()
                        .setPrivateParking(
                                ExtDataSchema.SiteParkingMessage
                                        .newBuilder()
                                        .setHasParking(true)
                                        .build()
                        )
                        .build()
        );
        site.setHasPrivateTerritory(false);
        site.setName("13");
        site.setOfficialUrl("14");
        Phase phase = new Phase(10);
        site.setPhases(Arrays.asList(phase));
        phase.setDescription("15");
        phase.setFinishDate(new Date());
        phase.setFinishDateCause("17");
        House house = new House(11);
        phase.setHouses(Arrays.asList(house));
        house.setAddress("18");
        house.setCoordinates(GeoPoint.getPoint(12.1f, 13.1f));
        house.setMaxHeight(14);
        house.setMinHeight(15);
        Photo photo = new Photo();
        photo.setTag("19");
        photo.setUrlPrefix("20");
        phase.setState(BuildingState.BUILT);
        Location location = new Location();
        location.setRegionGraphId(193295);
        location.setAccuracy(LocationAccuracy.EXACT);
        location.setHouseNum("10");
        location.setMetro(Collections.singletonList(MetroWithDistanceProtoConverter.constructNewMessage(1, 1, null, 12)));
        site.setLocation(location);
        Location matchFallbackLocation = new Location();
        location.setRegionGraphId(73771);
        location.setAccuracy(LocationAccuracy.OTHER);
        location.setManualPoint(GeoPoint.getPoint(55.65380335123396f, 37.267980058860694f));
        location.setGeocoderLocation("", GeoPoint.getPoint(55.654198f, 37.268631f));
        site.setHouseMatchFallbackLocation(matchFallbackLocation);
        site.setPhotos(Collections.singletonList(photo));
        Photo entrance = new Photo();
        photo.setUrlPrefix("adkslj");
        site.setSiteType(SiteType.SINGLE_HOUSE);
        site.setState(BuildingState.BUILT);
        site.setSaleStatus(SaleStatus.SOON_AVAILABLE);
        site.setMaternityFunds(Boolean.TRUE);
        site.setSiteMortgages(Collections.singletonList(new Mortgage(0L, 34L, "Ипотека")));

        site.setSales(Collections.singletonList(new Sale(40L, "Акция")));
        site.setGifts(Collections.singletonList(new Gift(50L, "Подарок")));
        site.setDiscounts(Collections.singletonList(new Discount(40L, "Скидка")));
        site.setInstallments(Collections.singletonList(new Installment(50L, "Рассрочка")));

        site.setDecorationInfo(Collections.singletonList(new DecorationInfo(Decoration.CLEAN, 4500, "Super euro")));
        return site;
    }
}
