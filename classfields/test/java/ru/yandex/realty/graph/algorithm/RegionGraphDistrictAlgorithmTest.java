package ru.yandex.realty.graph.algorithm;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.model.geometry.Polygon;
import ru.yandex.realty.model.region.NodeRgid;
import ru.yandex.realty.model.region.Regions;

/**
 * @author azakharov
 */
public class RegionGraphDistrictAlgorithmTest {

    @Test
    public void testCityDistrictWithBadName() {
        Node notDistrict = createCityDistrict(
                null, "горнолыжный комплекс Уктус", "Уктус",
                "в горнолыжном комплексе Уктус");
        Assert.assertFalse(RegionGraphDistrictAlgorithm.isValidDistrict(notDistrict));
    }

    @Test
    public void testSimpleCityDistrict() {
        String fullName = "Тимирязевский район";
        String display = "Тимирязевский";
        String locative = "в Тимирязевском районе";
        Node district = createCityDistrict(fullName, fullName, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testMoscowAdministrativeDistrict() {
        String official = "Центральный административный округ";
        String display = "ЦАО";
        String locative = "в Центральном административном округе";
        Node admDistrict = createCityDistrict(null, official, display, locative);
        // it is district, but district kind is DistrictKind.ADMINISTRATIVE_AREA so it is not visible as district
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(admDistrict));
    }

    @Test
    public void testSettlementDistrict() {
        String fullName = "поселок Чусовское Озеро";
        String display = "Чусовское Озеро";
        String locative = "в в поселке Чусовское Озеро";
        Node district = createCityDistrict(fullName, fullName, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testSimpleSubjectFederationDistrict() {
        String address = "Ленинский район";
        String official = "Ленинский муниципальный район";
        String display = "Ленинский";
        String locative = "в Ленинском районе";
        Node district = createSubjectFederationDistrict(address, official, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testSettlementSubjectFederationDistrict() {
        // for new moscow non administrative district (REALTY-11245, REALTY-11770):
        //   Внуковское
        //   Воскресенское
        //   Десёновское
        //   Кокошкино
        //   Марушкинское
        //   Московский
        //   Мосрентген
        //   Рязановское
        //   Сосенское
        //   Филимонковское
        //   Щербинка

        String fullName = "поселение Внуковское";
        String display = "Внуковское";
        String locative = "в Поселении Внуковское";
        Node district = createSubjectFederationDistrict(null, fullName, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testNewMoscowAdministrativeDistrict() {
        String fullName = "Троицкий административный округ";
        String display = "Троицкий округ";
        String locative = "в Троицком административном округе";
        Node district = createSubjectFederationDistrict(null, fullName, display, locative);
        // it is district, but district kind is DistrictKind.ADMINISTRATIVE_AREA so it is not visible as district
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testUrbanDistrictSimple() {
        String fullName = "городской округ Евпатория";
        String display = "Евпатория";
        String locative = "в Городском округе Евпатория";
        Node district = createSubjectFederationDistrict(null, fullName, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testUrbanDistrictComplex() {
        String fullName = "городской округ Красногорск";
        String display = "Красногорский";
        String locative = "в Городском округе Красногорске";
        Node district = createSubjectFederationDistrict(null, fullName, display, locative);
        Assert.assertTrue(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testOldMoscowSettlement() {
        String fullName = "поселок Главмосстроя";
        String display = "пос. Главмосстроя";
        Node district = createSubjectFederationDistrict(null, fullName, display, null);

        Node oldMoscow = Node.createNodeForGeoObjectType(GeoObjectType.CITY);
        oldMoscow.setId(NodeRgid.MOSCOW_WITHOUT_NEW_MOSCOW);
        oldMoscow.setGeoId(Regions.MOSCOW);

        district.addParentId(oldMoscow.getId());

        Assert.assertFalse(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    @Test
    public void testMoscowUrbanDistrict() {
        String fullName = "городской округ Московский";
        String display = "Московский окр.";
        Node district = createSubjectFederationDistrict(null, fullName, display, null);

        Node moscow = Node.createNodeForGeoObjectType(GeoObjectType.CITY);
        moscow.setId(NodeRgid.MOSCOW);
        moscow.setGeoId(Regions.MOSCOW);

        district.addParentId(moscow.getId());

        Assert.assertFalse(RegionGraphDistrictAlgorithm.isValidDistrict(district));
    }

    private static Node createCityDistrict(String address, String official, String display, String locative) {
        return createDistrict(GeoObjectType.CITY_DISTRICT, address, official, display, locative);
    }

    private static Node createSubjectFederationDistrict(String address, String official, String display, String locative) {
        return createDistrict(GeoObjectType.SUBJECT_FEDERATION_DISTRICT, address, official, display, locative);
    }

    private static Node createDistrict(GeoObjectType type, String address, String official, String display, String locative) {
        Node district = Node.createNodeForGeoObjectType(type);
        Name name = new Name();
        name.setDisplay(display);
        name.setAddress(address);
        name.setOfficial(official);
        name.setLocative(locative);
        district.setName(name);
        district.setGeometry(new Polygon(new float[]{1, 1, 6, 6}, new float[]{2, 7, 7, 2}));
        return district;
    }
}
