package ru.yandex.realty.graph.util;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.realty.graph.MutableRegionGraph;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.graph.core.Node;
import ru.yandex.realty.model.geometry.Polygon;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author azakharov
 */
public class NameCorrectorTest {

    private MutableRegionGraph regionGraph;
    private NameSplitter nameSplitter;

    @Before
    public void setUp() throws Exception {
        regionGraph = MutableRegionGraph.createEmptyRegionGraphWithAllFeatures();

        Node root = new Node();
        root.setId(0);
        Name rootName = new Name();
        rootName.setDisplay("world");
        root.setName(rootName);
        regionGraph.setRoot(root);

        Cut microDistricts = new Cut("микрорайон", list("мкр."), set(GeoObjectType.NOT_ADMINISTRATIVE_DISTRICT));
        Cut settlements = new Cut("поселок", list("пос."), set(GeoObjectType.NOT_ADMINISTRATIVE_DISTRICT));
        Cut gorodok = new Cut("городок", Collections.<String>emptyList(), set(GeoObjectType.NOT_ADMINISTRATIVE_DISTRICT));
        nameSplitter = new NameSplitter(Arrays.asList(microDistricts, settlements, gorodok));
    }

    @Test
    public void testCorrectMicrodistrict() {
        final String fullName = "микрорайон Кашира-2";
        final String displayName = "мкр. Кашира-2";
        final String locative = "в микрорайоне Кашира-2";
        Node node = createNode(GeoObjectType.NOT_ADMINISTRATIVE_DISTRICT, fullName, fullName, displayName, locative);
        regionGraph.addNode(node);

        NameCorrector.correct(regionGraph, nameSplitter);

        assertEquals("Кашира-2", node.getName().getDisplay());
    }

    @Test
    public void testCorrectSettlementCityDistrict() {
        final String fullName = "поселок БЗРИ";
        final String displayName = "пос. БЗРИ";
        Node node = createNode(GeoObjectType.CITY_DISTRICT, null, fullName, displayName, null);
        regionGraph.addNode(node);

        NameCorrector.correct(regionGraph, nameSplitter);
        assertEquals("БЗРИ", node.getName().getDisplay());
    }

    @Test
    public void testCorrectSettlementsCity() {
        final String fullName = "посёлок Внуково";
        final String displayName = "пос. Внуково";
        final String locative = "во Внуково";
        Node node = createNode(GeoObjectType.CITY, null, fullName, displayName, locative);
        regionGraph.addNode(node);

        NameCorrector.correct(regionGraph, nameSplitter);
        assertEquals("Внуково", node.getName().getDisplay());
    }

    @Test
    public void testCorrectUrbanDistrict() {
        final String fullName = "городской округ Звёздный городок";
        final String displayName = "ГО Звёздный городок";
        final String locative = "в Городском округе ЗАТО Звёздный городок";
        Node node = createNode(GeoObjectType.SUBJECT_FEDERATION_DISTRICT, null, fullName, displayName, locative);
        regionGraph.addNode(node);

        NameCorrector.correct(regionGraph, nameSplitter);
        assertEquals("Звёздный городок", node.getName().getDisplay());
    }

    @Test
    public void testCorrectCityDistrict() {
        final String address = "городской округ Улан-Удэ";
        final String official = "городской округ Улан-Удэ";
        final String displayName = "округ Улан-Удэ";
        Node node = createNode(GeoObjectType.SUBJECT_FEDERATION_DISTRICT, address, official, displayName, null);
        regionGraph.addNode(node);

        NameCorrector.correct(regionGraph, nameSplitter);
        assertEquals("Улан-Удэ (городской округ)", node.getName().getAddress());
        assertEquals("Улан-Удэ (городской округ)", node.getName().getOfficial());
    }

    private static Node createNode(GeoObjectType type, String address, String official, String display, String locative) {
        Node node = Node.createNodeForGeoObjectType(type);
        node.setId(42L);
        Name name = new Name();
        name.setDisplay(display);
        name.setAddress(address);
        name.setOfficial(official);
        name.setLocative(locative);
        node.setName(name);
        node.setGeometry(new Polygon(new float[]{1, 2, 6, 6}, new float[]{2, 7, 7, 2}));
        return node;
    }

}
