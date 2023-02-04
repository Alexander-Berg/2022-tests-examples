package ru.yandex.auto.indexer.lucene.handler.catalog;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.core.catalog.model.CatalogCardImpl;
import ru.yandex.auto.core.catalog.model.ConfigurationImpl;
import ru.yandex.auto.core.catalog.model.GenerationImpl;
import ru.yandex.auto.core.catalog.model.TechParameterImpl;
import ru.yandex.auto.core.handler.HandleUtils;

/** User: yan1984 Date: 28.04.2011 16:17:20 */
public class RelevanceSettingHandlerImplTest {
  private RelevanceSettingHandlerImpl relevanceSettingHandler;

  @Before
  public void setUp() throws Exception {
    relevanceSettingHandler = new RelevanceSettingHandlerImpl();
  }

  @Test
  public void testHandle() throws Exception {
    CatalogCardImpl catalogCard = new CatalogCardImpl();
    GenerationImpl generation = new GenerationImpl();
    catalogCard.setGenerationImpl(generation);
    generation.setYearFrom(1990);
    ConfigurationImpl configuration = new ConfigurationImpl();
    catalogCard.setConfigurationImpl(configuration);
    configuration.setBodyTypeCode("SEDAN");
    TechParameterImpl techParameter = new TechParameterImpl();
    catalogCard.setTechParameterImpl(techParameter);
    techParameter.setTransmissionCode("AUTOMATIC");
    techParameter.setGearTypeCode("FORWARD_CONTROL");
    techParameter.setDisplacementCm3(1800);
    techParameter.setEngineTypeCode("DIESEL");
    HandleUtils.readAndHandle(catalogCard, relevanceSettingHandler);
    Assert.assertEquals(899174, (int) catalogCard.getRelevance());
  }
}
