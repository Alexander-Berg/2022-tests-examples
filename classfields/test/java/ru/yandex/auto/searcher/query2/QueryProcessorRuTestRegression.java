package ru.yandex.auto.searcher.query2;

import com.google.common.io.LineReader;
import java.io.*;
import javax.xml.bind.JAXBException;
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
public class QueryProcessorRuTestRegression extends AbstractJUnit4SpringContextTests {

  @Autowired private QueryProcessor queryProcessor;
  @Autowired private QueryProcessor parallelQueryProcessor;

  protected QueryProcessor search() {
    return queryProcessor;
  }

  protected QueryProcessor wizard() {
    return parallelQueryProcessor;
  }

  @Ignore
  @Test
  public void testRegression()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {

    LineReader lr =
        new LineReader(
            new FileReader(
                new File("/Users/alesavin/tmp/auto/wizard.test/1mln_requests_16012014.txt")));
    OutputStream outputStream = new FileOutputStream("query_processor_regression.txt");
    //                new
    // FileOutputStream("/Users/alesavin/tmp/auto/wizard.test/1mln_responses_16012014_2.txt");

    String line;
    while ((line = lr.readLine()) != null) {
      SearchQuery searchQuery = wizard().processQuery(line);
      if (searchQuery == null
          || searchQuery.getParams() == null
          || searchQuery.getParams().getAllParams().size() == 0
          || !searchQuery.getUnclearWords().isEmpty()) continue;
      outputStream.write((line + "\t" + searchQuery.getParams() + "\n").getBytes());
    }

    outputStream.close();
  }
}
