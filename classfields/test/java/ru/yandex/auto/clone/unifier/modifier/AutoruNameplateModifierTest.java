package ru.yandex.auto.clone.unifier.modifier;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.catalog.grouping.CatalogCardGroupingService;
import ru.yandex.auto.core.model.UnifiedCarInfo;

@ContextConfiguration(locations = "/context/shard-test.xml")
@Ignore
public class AutoruNameplateModifierTest extends AbstractJUnit4SpringContextTests {
  @Autowired private CatalogCardGroupingService catalogCardGroupingService;
  private AutoruNameplateModifier autoruNameplateModifier;

  @Before
  public void setUp() {
    autoruNameplateModifier = new AutoruNameplateModifier();
    autoruNameplateModifier.setCatalogCardGroupingService(catalogCardGroupingService);
  }

  @Test
  public void nameModifyNameplate() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setMark("Audi");
    info.setModel("A3");
    info.setResourceId(1438536L);
    info.setTechParamId(20795557L);

    autoruNameplateModifier.modify(info);
  }
}
