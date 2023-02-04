package ru.yandex.auto.searcher.query2;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.io.LineReader;
import java.io.*;
import java.util.Map;
import javax.xml.bind.JAXBException;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.dictionary.FieldFactoryException;
import ru.yandex.auto.searcher.query.QueryProcessor;
import ru.yandex.auto.searcher.query.SearchQuery;

/** Created by IntelliJ IDEA. User: alesavin Date: 4/25/11 Time: 3:09 PM */
@ContextConfiguration(locations = "/ru/yandex/auto/searcher/query2/QueryProcessorRuTest.xml")
public class QueryProcessorRuTest extends AbstractJUnit4SpringContextTests {

  @Autowired private QueryProcessor queryProcessor;
  @Autowired private QueryProcessor parallelQueryProcessor;

  @Ignore
  @Test
  public void testParse()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {
    check(wizard().processQuery("big legend"), null);
    check(wizard().processQuery("ford focus"), ImmutableMap.of("mark", "FORD", "model", "FOCUS"));
    check(wizard().processQuery("форд фокус"), ImmutableMap.of("mark", "FORD", "model", "FOCUS"));
    check(wizard().processQuery("ford focus 1"), null);
    check(search().processQuery("ford focus 1"), ImmutableMap.of("mark", "FORD", "model", "FOCUS"));
  }

  protected QueryProcessor search() {
    return queryProcessor;
  }

  protected QueryProcessor wizard() {
    return parallelQueryProcessor;
  }

  public static void check(
      @Nullable final SearchQuery searchQuery, @Nullable final Map<String, String> expected) {
    if (searchQuery == null || searchQuery.getParams() == null) {
      if (expected == null) return;
      else throw new AssertionError("SearchQuery params is null");
    }

    Map<String, String> actual = Maps.newHashMap();
    for (String param : searchQuery.getParams().getAllParamNames())
      actual.put(param, searchQuery.getParams().getParam(param));

    MapDifference<String, String> md =
        expected == null
            ? Maps.difference(actual, Maps.newHashMap())
            : Maps.difference(actual, expected);
    if (!md.areEqual()) throw new AssertionError(md.toString());
  }

  @Ignore
  @Test
  public void testWizardQuality()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {

    LineReader lr =
        new LineReader(
            new FileReader(
                new File("/Users/alesavin/tmp/auto/wizard.test/1mln_requests_16012014.txt")));
    OutputStream outputStream =
        new FileOutputStream("/Users/alesavin/tmp/auto/wizard.test/1mln_responses_16012014_2.txt");

    String line;
    int count = 0;
    while ((line = lr.readLine()) != null) {
      /*
                  if (line.contains("-----------")) {
                      System.out.println(line);
                  }
      */
      SearchQuery searchQuery = parallelQueryProcessor.processQuery(line);
      if (searchQuery == null
          || searchQuery.getParams() == null
          || searchQuery.getParams().getAllParams().size() == 0
          || !searchQuery.getUnclearWords().isEmpty()) continue;
      outputStream.write((line + "\t" + searchQuery.getParams() + "\n").getBytes());
    }

    outputStream.close();
  }
}
