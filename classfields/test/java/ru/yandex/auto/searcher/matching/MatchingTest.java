package ru.yandex.auto.searcher.matching;

import java.io.File;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.catalog.grouping.CatalogCardGroupingService;
import ru.yandex.auto.core.catalog.model.CatalogCard;
import ru.yandex.auto.core.dictionary.LangsProvider;
import ru.yandex.auto.core.filter.complex_field.ComplexFieldsManager;
import ru.yandex.auto.core.filter.option.OptionManager;
import ru.yandex.auto.core.filter.option.OptionManagerHolder;
import ru.yandex.auto.core.region.Region;
import ru.yandex.auto.core.region.RegionService;
import ru.yandex.auto.core.region.RegionTree;
import ru.yandex.auto.core.stock.http.Currency;
import ru.yandex.auto.core.stock.http.PriceConverter;
import ru.yandex.auto.core.vendor.VendorManager;
import ru.yandex.auto.searcher.configuration.SearchConfiguration;
import ru.yandex.auto.searcher.configuration.SearchConfigurationBuilder;
import ru.yandex.auto.searcher.core.CarSearchParamsImpl;
import ru.yandex.auto.searcher.search.CatalogSearcher;
import ru.yandex.common.util.IOUtils;

/** User: daedra Date: 09.12.13 Time: 15:53 */
@ContextConfiguration(locations = "/matching.xml")
public class MatchingTest extends AbstractJUnit4SpringContextTests {

  @Autowired private CatalogCardGroupingService catalogCardGroupingService;
  @Autowired private CatalogSearcher catalogSearcher;
  private CarSearchParamsImpl carSearchParams;
  @Autowired private ComplexFieldsManager complexFieldsManager;
  @Autowired private SearchConfigurationBuilder searchConfigurationBuilder;
  @Autowired private RegionService regionService;
  @Autowired private RegionTree regionTree;
  @Autowired private VendorManager vendorManager;
  @Autowired private PriceConverter priceConverter;
  @Autowired private OptionManagerHolder optionManagerHolder;
  @Autowired private OptionManager optionManager;
  @Autowired private LangsProvider langsProvider;

  @Test
  @Ignore
  public void testMatching() throws Exception {
    String input = IOUtils.readWholeFile("/Users/daedra/Downloads/markModel.json");
    JSONArray inputArray = new JSONArray(input);
    JSONArray outputArray = new JSONArray();
    for (int i = 0; i < inputArray.length(); i++) {
      JSONObject obj = inputArray.getJSONObject(i);

      JSONObject resultObj = new JSONObject(obj.toString());
      String mark = obj.getString("mark");
      String model = obj.getString("model");
      int year = Integer.parseInt(obj.getString("prodyear"));

      carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
      carSearchParams.addMark(mark);
      carSearchParams.addModel(model);
      carSearchParams.setYears(year, year);

      SearchConfiguration searchConfiguration =
          searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

      List<CatalogCard> catalogCards = catalogSearcher.searchCatalogCards(searchConfiguration);
      if (!catalogCards.isEmpty()) {
        Collections.sort(
            catalogCards,
            new Comparator<CatalogCard>() {
              @Override
              public int compare(@NotNull CatalogCard o1, @NotNull CatalogCard o2) {
                int res = -(o1.getGeneration().getYearFrom() - o2.getGeneration().getYearFrom());
                if (res != 0) return res;
                return -(o1.getRelevance() - o2.getRelevance());
              }
            });
        long generationId = catalogCards.get(0).getGeneration().getId();
        resultObj.put("generation_id", generationId);
        outputArray.put(resultObj);
      }
    }
    File file = new File("/Users/daedra/Downloads/markModelResult.json");
    file.createNewFile();
    IOUtils.writeWholeFile(file, outputArray.toString().getBytes());
  }

  public void setCatalogCardGroupingService(CatalogCardGroupingService catalogCardGroupingService) {
    this.catalogCardGroupingService = catalogCardGroupingService;
  }

  public void setCatalogSearcher(CatalogSearcher catalogSearcher) {
    this.catalogSearcher = catalogSearcher;
  }

  public void setLangsProvider(LangsProvider langsProvider) {
    this.langsProvider = langsProvider;
  }

  @Before
  public void setUp() throws Exception {
    Mockito.when(complexFieldsManager.getSimpleTypes(Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(
            new Answer<Set<String>>() {
              @NotNull
              @Override
              public Set<String> answer(@NotNull InvocationOnMock invocation) throws Throwable {
                return new HashSet<>(Arrays.asList((String) invocation.getArguments()[0]));
              }
            });

    Mockito.when(regionService.getRegionTree()).thenReturn(regionTree);
    Mockito.when(regionTree.convertRegionIdsToRegions(Mockito.anyListOf(Integer.class)))
        .thenReturn(Collections.emptyList());
    Mockito.when(regionTree.getUnnestedRegions(Mockito.anyListOf(Region.class)))
        .thenReturn(Collections.emptyList());
    Mockito.when(
            priceConverter.convertFromCurrency(
                Currency.RUR,
                Mockito.anyDouble(),
                Mockito.any(ru.yandex.auto.core.stock.http.Currency.class)))
        .thenAnswer(
            new Answer<Double>() {
              @NotNull
              @Override
              public Double answer(@NotNull InvocationOnMock invocation) throws Throwable {
                return (Double) invocation.getArguments()[0];
              }
            });
    Mockito.when(optionManagerHolder.getOptionManager()).thenReturn(optionManager);
  }
}
