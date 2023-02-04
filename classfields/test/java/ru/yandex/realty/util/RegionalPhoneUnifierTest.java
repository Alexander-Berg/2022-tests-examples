package ru.yandex.realty.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.realty.model.offer.PhoneNumber;
import ru.yandex.realty.context.ProviderAdapter;
import ru.yandex.realty.graph.MutableRegionGraph;
import ru.yandex.realty.graph.RegionGraph;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.model.region.Regions;

/**
 * author: rmuzhikov
 */
public class RegionalPhoneUnifierTest {
    private RegionGraph regionGraph;
    private RegionalPhoneUnifier regionalPhoneUnifier;

    @Before
    public void setUp() throws Exception {
        MutableRegionGraph regionGraph = MutableRegionGraph.createEmptyRegionGraphWithAllFeatures();

        Node root = new Node();
        root.setId(0);
        Name rootName = new Name();
        rootName.setDisplay("world");
        root.setName(rootName);
        regionGraph.addNode(root);
        regionGraph.setRoot(root);

        Node ru = new Node();
        ru.setId(1);
        ru.setPhoneCode("7");
        ru.setGeoId(Regions.RUSSIA);
        ru.addParentId(regionGraph.getRoot().getId());
        regionGraph.getRoot().addChildrenId(ru.getId());
        ru.setGeoObjectType(GeoObjectType.COUNTRY);
        Name ruName = new Name();
        ruName.setDisplay("Россия");
        ru.setName(ruName);
        regionGraph.addNode(ru);

        Node ruChild = new Node();
        ruChild.setId(11);
        ruChild.setPhoneCode("812 499 498 496 495");
        Name ruChildName = new Name();
        ruChildName.setDisplay(ruChild.getPhoneCode());
        ruChild.setName(ruChildName);
        ruChild.addParentId(ru.getId());
        ru.addChildrenId(ruChild.getId());
        regionGraph.addNode(ruChild);

        Node ua = new Node();
        ua.setId(2);
        ua.setPhoneCode("380");
        ua.setGeoId(Regions.UKRAINE);
        ua.addParentId(regionGraph.getRoot().getId());
        regionGraph.getRoot().addChildrenId(ua.getId());
        ua.setGeoObjectType(GeoObjectType.COUNTRY);
        Name uaName = new Name();
        uaName.setDisplay("Украина");
        ua.setName(uaName);
        regionGraph.addNode(ua);

        Node uaChild = new Node();
        uaChild.setId(22);
        uaChild.setPhoneCode("44");
        Name uaChildName = new Name();
        uaChildName.setDisplay(uaChild.getPhoneCode());
        uaChild.setName(uaChildName);
        uaChild.addParentId(ua.getId());
        ua.addChildrenId(uaChild.getId());
        regionGraph.addNode(uaChild);

        Node by = new Node();
        by.setId(3);
        by.setPhoneCode("375");
        by.setGeoId(Regions.BELARUS);
        by.addParentId(regionGraph.getRoot().getId());
        regionGraph.getRoot().addChildrenId(by.getId());
        by.setGeoObjectType(GeoObjectType.COUNTRY);
        Name byName = new Name();
        byName.setDisplay("Белорусь");
        by.setName(byName);
        regionGraph.addNode(by);

        Node byChild = new Node();
        byChild.setId(33);
        byChild.setPhoneCode("17");
        Name byChildName = new Name();
        byChildName.setDisplay(byChild.getPhoneCode());
        byChild.setName(byChildName);
        byChild.addParentId(by.getId());
        by.addChildrenId(byChild.getId());
        regionGraph.addNode(byChild);

        Node kz = new Node();
        kz.setId(4);
        kz.setPhoneCode("7");
        kz.setGeoId(Regions.KAZAKHSTAN);
        kz.addParentId(regionGraph.getRoot().getId());
        regionGraph.getRoot().addChildrenId(kz.getId());
        kz.setGeoObjectType(GeoObjectType.COUNTRY);
        Name kzName = new Name();
        kzName.setDisplay("Казахстан");
        kz.setName(kzName);
        regionGraph.addNode(kz);

        Node kzChild = new Node();
        kzChild.setId(44);
        kzChild.setPhoneCode("7272");
        Name kzChildName = new Name();
        kzChildName.setDisplay(kzChild.getPhoneCode());
        kzChild.setName(kzChildName);
        kzChild.addParentId(kz.getId());
        kz.addChildrenId(kzChild.getId());
        regionGraph.addNode(kzChild);

        this.regionGraph = regionGraph;
        this.regionalPhoneUnifier = new RegionalPhoneUnifier(ProviderAdapter.<RegionGraph>create(regionGraph));
    }

    @Test
    public void testIsCityCode() throws Exception {
        Assert.assertTrue(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(1l), "812"));
        Assert.assertTrue(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(1l), "496"));
        Assert.assertFalse(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(1l), "813"));

        Assert.assertFalse(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(2l), "812"));
        Assert.assertFalse(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(2l), "813"));
        Assert.assertTrue(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(2l), "44"));

        Assert.assertFalse(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(4l), "44"));
        Assert.assertTrue(regionalPhoneUnifier.isCityCode(regionGraph.getNodeById(4l), "7272"));
    }

    @Test
    public void testUnify() throws Exception {
        assertPhoneUnification(new PhoneNumber("952", "3991438", "7"), Regions.RUSSIA);
        assertPhoneUnification(new PhoneNumber("812", "3991438", "7"), Regions.RUSSIA);

        assertPhoneUnification(new PhoneNumber("67", "4652925", "380"), Regions.UKRAINE);
        assertPhoneUnification(new PhoneNumber("44", "4652925", "380"), Regions.UKRAINE);

        assertPhoneUnification(new PhoneNumber("296", "253631", "375"), Regions.BELARUS);
        assertPhoneUnification(new PhoneNumber("17", "253631", "375"), Regions.BELARUS);

        assertPhoneUnification(new PhoneNumber("727", "3016019", "7"), Regions.KAZAKHSTAN);
        assertPhoneUnification(new PhoneNumber("7272", "253631", "7"), Regions.KAZAKHSTAN);

    }

    private void assertPhoneUnification(PhoneNumber p, int region) {
        Assert.assertEquals(p, regionalPhoneUnifier.unify("+" + p.getRegion() + " " + p.getCode() + " " + p.getNumber(), region));
        Assert.assertEquals(p, regionalPhoneUnifier.unify("+" + p.getRegion() + p.getCode() + p.getNumber(), region));
        Assert.assertEquals(p, regionalPhoneUnifier.unify("+" + p.getRegion() + "(" + p.getCode() + ")" + p.getNumber(), region));
        Assert.assertEquals(p, regionalPhoneUnifier.unify(RegionalPhoneUnifier.INNER_COUNTRY_CODES.get(region) + " " + p.getCode() + " " + p.getNumber(), region));
        Assert.assertEquals(p, regionalPhoneUnifier.unify(RegionalPhoneUnifier.INNER_COUNTRY_CODES.get(region) + p.getCode() + p.getNumber(), region));
        Assert.assertEquals(p, regionalPhoneUnifier.unify(RegionalPhoneUnifier.INNER_COUNTRY_CODES.get(region) + "(" + p.getCode() + ")" + p.getNumber(), region));
    }

}