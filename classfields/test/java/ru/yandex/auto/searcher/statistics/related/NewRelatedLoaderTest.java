package ru.yandex.auto.searcher.statistics.related;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.dictionary.FieldFactoryException;

/** Created by IntelliJ IDEA. User: alesavin Date: 4/25/11 Time: 3:09 PM */
@ContextConfiguration(
    locations = "/ru/yandex/auto/searcher/statistics/related/NewRelatedLoaderTest.xml")
public class NewRelatedLoaderTest extends AbstractJUnit4SpringContextTests {

  @Autowired private NewRelatedContainerHolder newRelatedContainerHolder;

  @Ignore
  @Test
  public void testLoad()
      throws IOException, FieldFactoryException, JAXBException, InterruptedException {
    NewRelatedContainer c = newRelatedContainerHolder.getNewRelatedContainer();
    System.out.println(c.getLocaleCodeToRelated().size());
  }
}
