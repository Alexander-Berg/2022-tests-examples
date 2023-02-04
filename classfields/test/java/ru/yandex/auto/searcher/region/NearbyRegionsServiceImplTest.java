package ru.yandex.auto.searcher.region;

import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.auto.core.region.Region;
import ru.yandex.auto.core.region.RegionTree;
import ru.yandex.auto.core.region.RegionType;
import ru.yandex.common.util.collections.CollectionFactory;

/** User: yan1984 Date: 16.08.2010 Time: 16:25:21 */
public class NearbyRegionsServiceImplTest {

  @Test
  public void testFillNearbyRegionsMap() {
    NearbyRegionsServiceImpl nearbyRegionsService = new NearbyRegionsServiceImpl();

    Region russiaRegion = new Region(1, "Россия", RegionType.COUNTRY, null, 0, 0);

    Region rostovRegion =
        new Region(2, "Ростовская область", RegionType.REGION, russiaRegion, 0, 0);
    Region rostovCityRegion = new Region(3, "Ростов", RegionType.CITY, rostovRegion, 0, 0);
    Region taganrogCityRegion = new Region(4, "Таганрог", RegionType.CITY, rostovRegion, 0, 0);
    rostovRegion.addChild(rostovCityRegion);
    rostovRegion.addChild(taganrogCityRegion);

    Region moscowCityRegion = new Region(5, "Москва", RegionType.CITY, russiaRegion, 0, 0);

    russiaRegion.addChild(rostovRegion);
    russiaRegion.addChild(moscowCityRegion);

    StringBuilder inputData = new StringBuilder();
    inputData.append(
        "2[^2,3]:3,5,1\n"); // дочерние регионы Ростовской области кроме Ростова-на-Дону
    inputData.append("2[^]:5,1\n"); // Ростов-на-Дону или Ростовская область

    nearbyRegionsService.updateNearbyRegions(
        new StringReader(inputData.toString()), new RegionTree(russiaRegion));

    Assert.assertEquals(
        CollectionFactory.newArrayList(new Region[] {moscowCityRegion, russiaRegion}),
        nearbyRegionsService.getNearbyRegions(rostovCityRegion));

    Assert.assertEquals(
        CollectionFactory.newArrayList(
            new Region[] {rostovCityRegion, moscowCityRegion, russiaRegion}),
        nearbyRegionsService.getNearbyRegions(taganrogCityRegion));
  }
}
