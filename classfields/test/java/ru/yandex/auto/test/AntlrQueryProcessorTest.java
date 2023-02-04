package ru.yandex.auto.test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import ru.yandex.auto.core.dictionary.Type;
import ru.yandex.auto.searcher.core.CarSearchParams;
import ru.yandex.auto.searcher.query.QueryProcessor;
import ru.yandex.auto.searcher.query.SearchQuery;
import ru.yandex.auto.searcher.query.auto.MarkType;
import ru.yandex.auto.searcher.query.auto.ModelType;
import ru.yandex.auto.searcher.query2.dictionary.MapBasedDictionaryImpl;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.functional.Function;

/** User: yan1984 Date: 25.01.2012 10:33:06 */
public class AntlrQueryProcessorTest extends AbstractDependencyInjectionSpringContextTests {
  private QueryProcessor antlrQueryProcessor;
  private Function<Pair<String, Object>, MarkType> markDictionary;
  private Function<Pair<String, Object>, List<ModelType>> modelDictionary;

  public AntlrQueryProcessorTest() {
    setAutowireMode(AUTOWIRE_BY_NAME);
  }

  public void setAntlrQueryProcessor(QueryProcessor antlrQueryProcessor) {
    this.antlrQueryProcessor = antlrQueryProcessor;
  }

  public void setMarkDictionary(Function<Pair<String, Object>, MarkType> markDictionary) {
    this.markDictionary = markDictionary;
  }

  public void setModelDictionary(Function<Pair<String, Object>, List<ModelType>> modelDictionary) {
    this.modelDictionary = modelDictionary;
  }

  @Override
  protected void onTearDown() throws Exception {
    Mockito.reset(markDictionary, modelDictionary);
  }

  @Test
  public void testMarkModelQuery() throws Exception {
    mockMarkTypes(
        createMarkType(
            "FORD",
            CollectionFactory.list("форд"),
            CollectionFactory.list(createModelType("FOCUS", CollectionFactory.list("фокус")))));

    SearchQuery searchQuery = antlrQueryProcessor.processQuery("форд фокус");
    Assert.assertNull(searchQuery.getRedir());
    Assert.assertEquals(0, searchQuery.getUnclearWords().size());
    Assert.assertEquals("FORD", searchQuery.getParams().getMark());
    Assert.assertEquals("FOCUS", searchQuery.getParams().getModel());
  }

  @Test
  public void testYearInterval() throws Exception {
    SearchQuery searchQuery = antlrQueryProcessor.processQuery("1999-2010");
    Assert.assertEquals("1999", searchQuery.getParams().getParam(CarSearchParams.YEAR_FROM));
    Assert.assertEquals("2010", searchQuery.getParams().getParam(CarSearchParams.YEAR_TO));
  }

  @Test
  public void testAmbiguousAliases() throws Exception {
    mockMarkTypes(
        createMarkType(
            "GINETTA",
            CollectionFactory.list("джинетта"),
            CollectionFactory.list(createModelType("G", CollectionFactory.list("г")))));

    SearchQuery searchQuery = antlrQueryProcessor.processQuery("г 1998 г");
    Assert.assertEquals("G", searchQuery.getParams().getModel());
    Assert.assertEquals("1998", searchQuery.getParams().getParam(CarSearchParams.YEAR_FROM));
    Assert.assertEquals("1998", searchQuery.getParams().getParam(CarSearchParams.YEAR_TO));
  }

  @Test
  public void testAmbiguousAliases2() throws Exception {
    mockMarkTypes(
        createMarkType(
            "FORD",
            CollectionFactory.list("форд"),
            CollectionFactory.list(createModelType("500", CollectionFactory.list("500")))));

    SearchQuery searchQuery = antlrQueryProcessor.processQuery("форд 500 от 500 тысяч рублей");
    Assert.assertEquals("FORD", searchQuery.getParams().getMark());
    Assert.assertEquals("500", searchQuery.getParams().getModel());
    Assert.assertEquals("500000", searchQuery.getParams().getParam(CarSearchParams.PRICE_FROM));
    Assert.assertNull(searchQuery.getParams().getParam(CarSearchParams.PRICE_TO, true));
    Assert.assertEquals("RUR", searchQuery.getParams().getParam(CarSearchParams.CURRENCY));
  }

  @Test
  public void testNumber() throws Exception {
    SearchQuery searchQuery = antlrQueryProcessor.processQuery("от 500 рублей");
    Assert.assertEquals("500", searchQuery.getParams().getParam(CarSearchParams.PRICE_FROM));

    searchQuery = antlrQueryProcessor.processQuery("до 3 литров");
    Assert.assertEquals("3000", searchQuery.getParams().getParam(CarSearchParams.DISPLACEMENT_TO));
  }

  @Test
  public void testLongDash() throws Exception {
    long start = System.currentTimeMillis();
    antlrQueryProcessor.processQuery(
        "------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    Assert.assertTrue((System.currentTimeMillis() - start) < 1000);
  }

  @Test
  public void testYearsAndModel() throws Exception {
    mockMarkTypes(
        createMarkType(
            "FORD",
            CollectionFactory.list("форд"),
            CollectionFactory.list(createModelType("500", CollectionFactory.list("500")))));

    SearchQuery searchQuery = antlrQueryProcessor.processQuery("форд 2007-2010");
    Assert.assertEquals("2007", searchQuery.getParams().getParam(CarSearchParams.YEAR_FROM));
    Assert.assertEquals("2010", searchQuery.getParams().getParam(CarSearchParams.YEAR_TO));
    Assert.assertEquals("FORD", searchQuery.getParams().getParam(CarSearchParams.MARK));
  }

  @Test
  public void testMarkModel() throws Exception {
    SearchQuery searchQuery = antlrQueryProcessor.processQuery("big legend");
    System.out.println(searchQuery.getParams().getMark());
  }

  private void mockMarkTypes(@NotNull MarkType... markTypes) {
    final MapBasedDictionaryImpl<MarkType> markTypeMapBasedDictionary =
        new MapBasedDictionaryImpl<MarkType>();
    final MapBasedDictionaryImpl<List<ModelType>> modelListMapBasedDictionary =
        new MapBasedDictionaryImpl<List<ModelType>>();

    ConcurrentHashMap<String, MarkType> markMap = new ConcurrentHashMap<String, MarkType>();
    ConcurrentHashMap<String, List<ModelType>> modelMap =
        new ConcurrentHashMap<String, List<ModelType>>();
    for (MarkType markType : markTypes) {
      for (String str : markType.getStringVariants()) {
        markMap.put(str, markType);
      }
      for (ModelType modelType : markType.getModels()) {
        for (String str : modelType.getStringVariants()) {
          modelMap.put(str, Collections.singletonList(modelType));
        }
      }
    }
    markTypeMapBasedDictionary.setMap(markMap);
    modelListMapBasedDictionary.setMap(modelMap);

    Mockito.when(this.markDictionary.apply(Mockito.any(Pair.class)))
        .thenAnswer(
            new Answer<MarkType>() {
              @Override
              public MarkType answer(@NotNull InvocationOnMock invocationOnMock) throws Throwable {
                return markTypeMapBasedDictionary.apply(
                    Pair.of(
                        ((Pair<String, Object>) invocationOnMock.getArguments()[0]).getFirst(),
                        null));
              }
            });

    Mockito.when(this.modelDictionary.apply(Mockito.any(Pair.class)))
        .thenAnswer(
            new Answer<List<ModelType>>() {
              @Override
              public List<ModelType> answer(@NotNull InvocationOnMock invocationOnMock)
                  throws Throwable {
                return modelListMapBasedDictionary.apply(
                    Pair.of(
                        ((Pair<String, Object>) invocationOnMock.getArguments()[0]).getFirst(),
                        null));
              }
            });
  }

  @NotNull
  private MarkType createMarkType(
      String code, @NotNull List<String> aliases, @NotNull List<ModelType> modelTypes) {
    Type type = createType(code, aliases);
    MarkType markType = new MarkType(type);
    for (ModelType modelType : modelTypes) {
      modelType.setMark(markType);
    }
    markType.setModels(modelTypes);
    return markType;
  }

  @Nullable
  private ModelType createModelType(String code, @NotNull List<String> aliases) {
    return new ModelType(createType(code, aliases), null);
  }

  @NotNull
  private Type createType(String code, @NotNull List<String> aliases) {
    Type type = new Type(code, Collections.emptyMap(), Collections.emptyMap());
    for (String alias : aliases) {
      type.addStringVariant(alias);
    }
    return type;
  }

  @NotNull
  @Override
  protected String[] getConfigPaths() {
    return new String[] {"/ru/yandex/auto/searcher/query/context.xml"};
  }
}
