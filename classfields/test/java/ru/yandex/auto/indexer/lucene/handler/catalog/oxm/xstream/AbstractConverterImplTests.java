package ru.yandex.auto.indexer.lucene.handler.catalog.oxm.xstream;

import java.io.InputStream;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import ru.yandex.auto.core.catalog.model.CatalogCardImpl;
import ru.yandex.auto.indexer.lucene.handler.catalog.oxm.xstream.builder.CatalogByVerbaServiceBuilder;
import ru.yandex.verba2.model.Service;
import ru.yandex.verba2.parse.SaxVerbaParser;
import ru.yandex.verba2.parse.VerbaParser;

/** User: yan1984 Date: 29.04.2011 14:00:15 */
public abstract class AbstractConverterImplTests
    extends AbstractDependencyInjectionSpringContextTests {
  public AbstractConverterImplTests() {
    setAutowireMode(AUTOWIRE_BY_NAME);
  }

  @NotNull
  @Override
  protected String[] getConfigPaths() {
    return new String[] { // todo provide test context
    };
  }

  @NotNull
  protected List<CatalogCardImpl> unmarshall(
      @NotNull CatalogByVerbaServiceBuilder serviceBuilder, InputStream is) throws Exception {
    VerbaParser parser = new SaxVerbaParser();

    Service service = parser.parseXml(is);

    return serviceBuilder.buildCatalogCards(service);
  }
}
