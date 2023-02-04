package ru.yandex.auto.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import ru.yandex.auto.core.filter.complex_field.ComplexFieldsManager;
import ru.yandex.auto.core.util.IntegerInterval;
import ru.yandex.auto.core.vendor.VendorManager;
import ru.yandex.auto.searcher.CustomsState;
import ru.yandex.auto.searcher.configuration.GenerationInfo;
import ru.yandex.auto.searcher.configuration.SearchConfiguration;
import ru.yandex.auto.searcher.core.CarSearchParams;
import ru.yandex.auto.searcher.core.ConditionCreator;
import ru.yandex.auto.searcher.core.SearchContext;
import ru.yandex.common.util.collections.CollectionFactory;

/** User: yan1984 Date: 24.05.2011 19:14:42 */
public abstract class AbstractSearchConfigurationBasedTests {
  private SearchConfiguration searchConfiguration;

  protected SearchConfiguration getSearchConfiguration() {
    return searchConfiguration;
  }

  @Before
  public void setUp() {
    ConditionCreator conditionCreator = Mockito.mock(ConditionCreator.class);
    Mockito.when(conditionCreator.getLowerTopDayRange(Mockito.anyInt())).thenReturn(60l);

    ComplexFieldsManager complexFieldsManager = Mockito.mock(ComplexFieldsManager.class);

    SearchContext searchContext = Mockito.mock(SearchContext.class);
    Mockito.when(searchContext.getConditionCreator()).thenReturn(conditionCreator);
    Mockito.when(searchContext.getComplexFieldsManager()).thenReturn(complexFieldsManager);

    VendorManager vendorManager = Mockito.mock(VendorManager.class);
    CarSearchParams carSearchParams = Mockito.mock(CarSearchParams.class);

    searchConfiguration = Mockito.mock(SearchConfiguration.class, new DoesNothing());
    Mockito.when(searchConfiguration.getParamIds()).thenReturn(Collections.emptyList());
    Mockito.when(searchConfiguration.getParamIdLowerBound()).thenReturn(null);
    Mockito.when(searchConfiguration.getParamMark()).thenReturn(CollectionFactory.set("FORD"));
    Mockito.when(searchConfiguration.getParamModels())
        .thenReturn(CollectionFactory.set("FOCUS", "MONDEO"));
    Mockito.when(searchConfiguration.getPriceFromInRubles()).thenReturn(10000);
    Mockito.when(searchConfiguration.getPriceToInRubles()).thenReturn(100000);
    Mockito.when(searchConfiguration.getClusterId()).thenReturn(null);
    Mockito.when(searchConfiguration.getVendors()).thenReturn(Collections.emptyList());
    Mockito.when(searchConfiguration.getParams()).thenReturn(carSearchParams);
    Mockito.when(searchConfiguration.getBodyTypes()).thenReturn(Collections.emptyList());
    Mockito.when(searchConfiguration.getEngineTypes()).thenReturn(Collections.emptyList());
    Mockito.when(searchConfiguration.getSearchContext()).thenReturn(searchContext);
    Mockito.when(searchConfiguration.getKmAgeInKm()).thenReturn(new IntegerInterval(0, 100000));
    Mockito.when(searchConfiguration.getYearFrom()).thenReturn(1990);
    Mockito.when(searchConfiguration.getYearTo()).thenReturn(null);
    Mockito.when(searchConfiguration.getDisplacementFrom()).thenReturn(null);
    Mockito.when(searchConfiguration.getDisplacementTo()).thenReturn(null);
    Mockito.when(searchConfiguration.getSearchContext().getVendorManager())
        .thenReturn(vendorManager);
    Mockito.when(searchConfiguration.getCustomHouseState()).thenReturn(CustomsState.UNDEFINED);
    Mockito.when(searchConfiguration.getCylyndersOrder()).thenReturn(Collections.emptyList());
    Mockito.when(searchConfiguration.getParamOptions()).thenReturn(Collections.emptyMap());
    Mockito.when(searchConfiguration.getBodySize()).thenReturn(Collections.emptyList());
    Map<String, Map<String, Map<String, Set<Long>>>> superGenerationMap =
        new HashMap<String, Map<String, Map<String, Set<Long>>>>();
    Map<String, Map<String, Set<Long>>> markMap = new HashMap<String, Map<String, Set<Long>>>();
    markMap.put("FOCUS", new HashMap<String, Set<Long>>());
    markMap.put("MONDEO", new HashMap<String, Set<Long>>());
    superGenerationMap.put("FORD", markMap);
    Mockito.when(searchConfiguration.getSuperGenerationsMap()).thenReturn(superGenerationMap);
    Map<String, Map<String, Set<GenerationInfo>>> generationsMap =
        new HashMap<String, Map<String, Set<GenerationInfo>>>();
    Mockito.when(searchConfiguration.getGenerationsMap()).thenReturn(generationsMap);
    Mockito.when(searchConfiguration.getDoorsCount()).thenReturn(Collections.emptySet());
  }
}
