package ru.yandex.realty.graph.region;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.realty.graph.region.CustomRegionAttribute;
import ru.yandex.realty.graph.region.Region;
import ru.yandex.realty.graph.region.RegionTree;
import ru.yandex.realty.graph.region.RegionType;

import java.io.File;

/**
 * @author Vladimir Yakunin (vyakunin@yandex-team.ru)
 */
public class XmlRegionTreeBuilderTest {
    private XmlRegionTreeBuilder xmlRegionTreeBuilder;

    @Before
    public void setUp() throws Exception {
        xmlRegionTreeBuilder = new XmlRegionTreeBuilder(new File("../realty-common/test-data/test-regions.xml"));
    }

    @Test
    public void testBuildRegionTree() throws Exception {
        RegionTree tree = xmlRegionTreeBuilder.buildRegionTree();
        validateTree(tree);
    }

    private static void validateTree(RegionTree tree) {
        Region earthRegion = tree.getRegion(10000);
        Region europeRegion = tree.getRegion(10001);

        assert earthRegion != null;
        assert europeRegion != null;

        Assert.assertEquals("Earth", earthRegion.getName());
        Assert.assertEquals("Europe", europeRegion.getName());
        Assert.assertEquals(RegionType.OTHERS_UNIVERSAL, earthRegion.getType());
        Assert.assertEquals(RegionType.CONTINENT, europeRegion.getType());

        Assert.assertEquals(null, earthRegion.getParent());
        Assert.assertEquals(earthRegion, europeRegion.getParent());

        Assert.assertEquals("0", earthRegion.getCustomAttributeValue(CustomRegionAttribute.MAPS));
        Assert.assertEquals("1", europeRegion.getCustomAttributeValue(CustomRegionAttribute.MAPS));

        Assert.assertEquals("1", europeRegion.getCustomAttributeValue(CustomRegionAttribute.LAT));
        Assert.assertEquals("2", europeRegion.getCustomAttributeValue(CustomRegionAttribute.LON));
        Assert.assertEquals("3", europeRegion.getCustomAttributeValue(CustomRegionAttribute.ZOOM));
        Assert.assertEquals("812", europeRegion.getCustomAttributeValue(CustomRegionAttribute.PHONE_CODE));

    }
}
