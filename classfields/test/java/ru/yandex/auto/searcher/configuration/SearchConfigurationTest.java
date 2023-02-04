package ru.yandex.auto.searcher.configuration;

import static ru.yandex.common.util.collections.CollectionFactory.set;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.dictionary.LangsProvider;
import ru.yandex.auto.core.filter.complex_field.ComplexFieldsManager;
import ru.yandex.auto.core.filter.complex_field.ComplexType;
import ru.yandex.auto.core.filter.option.OptionManager;
import ru.yandex.auto.core.filter.option.OptionManagerHolder;
import ru.yandex.auto.core.region.Region;
import ru.yandex.auto.core.region.RegionService;
import ru.yandex.auto.core.region.RegionTree;
import ru.yandex.auto.core.stock.http.PriceConverter;
import ru.yandex.auto.core.vendor.VendorManager;
import ru.yandex.auto.searcher.core.CarSearchParams;
import ru.yandex.auto.searcher.core.CarSearchParamsImpl;

/** User: daedra Date: 4/3/12 3:13 PM */
@ContextConfiguration(locations = "/context.xml")
@Ignore
public class SearchConfigurationTest extends AbstractJUnit4SpringContextTests {

  private CarSearchParamsImpl carSearchParams = Mockito.mock(CarSearchParamsImpl.class);

  @Autowired private ComplexFieldsManager complexFieldsManager;
  @Autowired private SearchConfigurationBuilder searchConfigurationBuilder;
  @Autowired private RegionService regionService;
  @Autowired private RegionTree regionTree;
  @Autowired private VendorManager vendorManager;
  @Autowired private PriceConverter priceConverter;
  @Autowired private OptionManagerHolder optionManagerHolder;
  @Autowired private OptionManager optionManager;
  @Autowired private LangsProvider langsProvider;

  public void setRegionTree(RegionTree regionTree) {
    this.regionTree = regionTree;
  }

  public void setPriceConverter(PriceConverter priceConverter) {
    this.priceConverter = priceConverter;
  }

  public void setVendorManager(VendorManager vendorManager) {
    this.vendorManager = vendorManager;
  }

  public void setRegionService(RegionService regionService) {
    this.regionService = regionService;
  }

  public void setSearchConfigurationBuilder(SearchConfigurationBuilder searchConfigurationBuilder) {
    this.searchConfigurationBuilder = searchConfigurationBuilder;
  }

  public void setComplexFieldsManager(ComplexFieldsManager complexFieldsManager) {
    this.complexFieldsManager = complexFieldsManager;
  }

  public void setOptionManagerHolder(OptionManagerHolder optionManagerHolder) {
    this.optionManagerHolder = optionManagerHolder;
  }

  public void setOptionManager(OptionManager optionManager) {
    this.optionManager = optionManager;
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

    Mockito.when(
            complexFieldsManager.getLowestComplexTypeByFieldNameAndSimpleTypeCode(
                Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(
            new Answer<ComplexType>() {
              @Nullable
              @Override
              public ComplexType answer(@NotNull final InvocationOnMock invocation)
                  throws Throwable {
                String res = (String) invocation.getArguments()[1];
                return new ComplexType(null, null, res, null, null);
              }
            });

    Mockito.when(regionService.getRegionTree()).thenReturn(regionTree);
    Mockito.when(regionTree.convertRegionIdsToRegions(Mockito.anyListOf(Integer.class)))
        .thenReturn(Collections.emptyList());
    Mockito.when(regionTree.getUnnestedRegions(Mockito.anyListOf(Region.class)))
        .thenReturn(Collections.emptyList());

    Mockito.when(
            priceConverter.convertToCurrency(
                Mockito.any(ru.yandex.auto.core.stock.http.Currency.class),
                Mockito.anyDouble(),
                Mockito.any(ru.yandex.auto.core.stock.http.Currency.class)))
        .thenAnswer(
            new Answer<Double>() {
              @NotNull
              @Override
              public Double answer(@NotNull InvocationOnMock invocationOnMock) throws Throwable {
                return (Double) invocationOnMock.getArguments()[1];
              }
            });

    Mockito.when(optionManagerHolder.getOptionManager()).thenReturn(optionManager);
  }

  @Test
  public void testQueryParams() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(
        CarSearchParams.TEXT,
        "форд фокус седан автомат 1.8 левый красный наличие только с фоткой 1993-1994 800000-900000 руб");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    Assert.assertEquals(searchConfiguration.getParamMark().size(), 1);
    Assert.assertEquals(searchConfiguration.getParamModels().size(), 1);
    Assert.assertNotNull(searchConfiguration.getSuperGenerationsMap().get("FORD"));
    Assert.assertEquals(
        searchConfiguration.getSuperGenerationsMap().get("FORD").keySet().iterator().next(),
        "FOCUS");
    Assert.assertEquals(searchConfiguration.getParamBodyTypes().get(0), "SEDAN");
    Assert.assertEquals(searchConfiguration.getTransmissions().get(0), "AUTOMATIC");
    Assert.assertEquals(searchConfiguration.getDisplacementFrom().intValue(), 1800);
    Assert.assertEquals(searchConfiguration.getSteeringWheels().get(0), "LEFT");
    Collection<String> colors = searchConfiguration.getParamColors();
    Assert.assertEquals(1, colors.size());
    Assert.assertEquals("EE1D19", colors.iterator().next());
    Assert.assertTrue(searchConfiguration.isInStock());
    Assert.assertTrue(searchConfiguration.isPhotoRequired());
    Assert.assertEquals(searchConfiguration.getYearFrom().intValue(), 1993);
    Assert.assertEquals(searchConfiguration.getYearTo().intValue(), 1994);
    Assert.assertEquals(searchConfiguration.getPriceFrom().intValue(), 800000);
    Assert.assertEquals(searchConfiguration.getPriceTo().intValue(), 900000);
  }

  @Test
  public void testMergeParams() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(
        CarSearchParams.TEXT,
        "форд фокус седан автомат 1.8 левый красный 1993-1994 800000-900000 руб");
    carSearchParams.addParam(CarSearchParams.MARK, "OPEL");
    carSearchParams.addParam(CarSearchParams.MODEL, "ASTRA");
    carSearchParams.addParam(CarSearchParams.BODY_TYPE, "HATCHBACK");
    carSearchParams.addParam(CarSearchParams.TRANSMISSION, "MANUAL");
    carSearchParams.addParam(CarSearchParams.DISPLACEMENT_FROM, "1900");
    carSearchParams.addParam(CarSearchParams.DISPLACEMENT_TO, "1950");
    carSearchParams.addParam(CarSearchParams.STEERING_WHEEL, "RIGHT");
    carSearchParams.addParam(CarSearchParams.COLOR, "0000CC");
    carSearchParams.addParam(CarSearchParams.IN_STOCK, "on");
    carSearchParams.addParam(CarSearchParams.IMAGE, "on");
    carSearchParams.addParam(CarSearchParams.YEAR_FROM, "2000");
    carSearchParams.addParam(CarSearchParams.YEAR_TO, "2001");
    carSearchParams.addParam(CarSearchParams.PRICE_FROM, "1000000");
    carSearchParams.addParam(CarSearchParams.PRICE_TO, "120000");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    Assert.assertEquals(searchConfiguration.getParamMark().size(), 2);
    Assert.assertEquals(searchConfiguration.getParamModels().size(), 2);
    Assert.assertNotNull(searchConfiguration.getSuperGenerationsMap().get("FORD"));
    Assert.assertEquals(
        searchConfiguration.getSuperGenerationsMap().get("FORD").keySet().iterator().next(),
        "FOCUS");
    Assert.assertEquals(set("SEDAN", "HATCHBACK"), set(searchConfiguration.getParamBodyTypes()));
    Assert.assertEquals(set("MANUAL", "AUTOMATIC"), set(searchConfiguration.getTransmissions()));
    Assert.assertEquals(searchConfiguration.getDisplacementFrom().intValue(), 1800, 50);
    Assert.assertEquals(searchConfiguration.getDisplacementTo().intValue(), 1800, 50);
    Assert.assertEquals(set("LEFT", "RIGHT"), set(searchConfiguration.getSteeringWheels()));
    Assert.assertEquals(set("EE1D19"), searchConfiguration.getParamColors());
    Assert.assertTrue(searchConfiguration.isInStock());
    Assert.assertTrue(searchConfiguration.isPhotoRequired());
    Assert.assertEquals(searchConfiguration.getYearFrom().intValue(), 1993);
    Assert.assertEquals(searchConfiguration.getYearTo().intValue(), 1994);
    Assert.assertEquals(searchConfiguration.getPriceFrom().intValue(), 800000);
    Assert.assertEquals(searchConfiguration.getPriceTo().intValue(), 900000);
  }

  @Test
  public void testMultipleColorsInText() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(CarSearchParams.TEXT, "красный синий");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    SortedSet<String> colors = new TreeSet<String>(searchConfiguration.getParamColors());
    Assert.assertEquals(2, colors.size());
    Iterator<String> it = colors.iterator();
    Assert.assertEquals("0000CC", it.next());
    Assert.assertEquals("EE1D19", it.next());
  }

  @Test
  public void testMultipleColorsInParams() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(CarSearchParams.COLOR, "0000CC");
    carSearchParams.addParam(CarSearchParams.COLOR, "EE1D19");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    SortedSet<String> colors = new TreeSet<String>(searchConfiguration.getParamColors());
    Assert.assertEquals(2, colors.size());
    Iterator<String> it = colors.iterator();
    Assert.assertEquals("0000CC", it.next());
    Assert.assertEquals("EE1D19", it.next());
  }

  @Test
  public void testMultipleColorsBothInParamsAndText() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(CarSearchParams.TEXT, "красный");
    carSearchParams.addParam(CarSearchParams.COLOR, "0000CC");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    SortedSet<String> colors = new TreeSet<String>(searchConfiguration.getParamColors());
    Assert.assertEquals(1, colors.size());
    Iterator<String> it = colors.iterator();
    Assert.assertEquals("EE1D19", it.next());
  }

  @Test
  public void testMergeParamsColor() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(CarSearchParams.TEXT, "красный");
    carSearchParams.addParam(CarSearchParams.COLOR, "0000CC");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    Assert.assertEquals(set("EE1D19"), searchConfiguration.getParamColors());
  }

  @Test
  public void testMergeParamsYear() {
    carSearchParams = new CarSearchParamsImpl(regionTree, vendorManager, langsProvider);
    carSearchParams.addParam(CarSearchParams.TEXT, "2014");
    carSearchParams.addParam(CarSearchParams.YEAR_FROM, "2013");
    carSearchParams.addParam(CarSearchParams.YEAR_TO, "2013");

    SearchConfiguration searchConfiguration =
        searchConfigurationBuilder.buildSearchConfiguration(carSearchParams, true);

    Assert.assertEquals(2014, searchConfiguration.getYearFrom().intValue());
    Assert.assertEquals(2014, searchConfiguration.getYearTo().intValue());
  }
}
