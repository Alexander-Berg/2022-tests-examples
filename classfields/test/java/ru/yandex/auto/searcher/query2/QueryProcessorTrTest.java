package ru.yandex.auto.searcher.query2;

import static ru.yandex.auto.searcher.query2.QueryProcessorRuTest.check;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.dictionary.FieldFactoryException;
import ru.yandex.auto.searcher.query.QueryProcessor;

/** Created by IntelliJ IDEA. User: alesavin Date: 4/25/11 Time: 3:09 PM */
@ContextConfiguration(locations = "/ru/yandex/auto/searcher/query2/QueryProcessorTrTest.xml")
public class QueryProcessorTrTest extends AbstractJUnit4SpringContextTests {

  @Autowired private QueryProcessor queryProcessor;
  @Autowired private QueryProcessor parallelQueryProcessor;

  private QueryProcessor search() {
    return queryProcessor;
  }

  private QueryProcessor wizard() {
    return parallelQueryProcessor;
  }

  @Ignore
  @Test
  public void testParse()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {
    check(wizard().processQuery("big legend"), null);
    check(wizard().processQuery("ford focus"), ImmutableMap.of("mark", "FORD", "model", "FOCUS"));
    check(wizard().processQuery("форд фокус"), null);
    check(wizard().processQuery("ford focus 1"), null);
    /*
            check(search().processQuery("ford focus 1"),
                    ImmutableMap.of("mark", "FORD", "model", "FOCUS"));
    */
  }

  @Ignore
  @Test
  public void testParse2()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {
    check(wizard().processQuery("tr  gg"), null);
  }
}
