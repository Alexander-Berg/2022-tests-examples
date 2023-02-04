package ru.yandex.auto.clone.unifier.dicunifier;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.unifier.UnunifiedFieldManager;

/** User: daedra Date: 23.05.13 Time: 17:22 */
@ContextConfiguration(locations = "/context/unifiers.xml")
@Ignore
public class MarkModelParserTest extends AbstractJUnit4SpringContextTests {
  @Autowired private MarkModelParser markModelParser;
  @Autowired private UnunifiedFieldManager ununifiedFieldManager;

  @Test
  public void testCorollaE12() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setMark("Toyota");
    info.setModel("Corolla (E12)");
    markModelParser.unify(info, ununifiedFieldManager);

    Assert.assertNull(info.getModel());
    Assert.assertEquals("TOYOTA", info.getMark());
    Assert.assertEquals("Corolla (E12)", info.getRawModel());
  }

  @Test
  public void testCaptivatedKaptur() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setMark("Renault");
    info.setModel("Captur");

    markModelParser.unify(info, ununifiedFieldManager);

    Assert.assertEquals("CAPTUR", info.getModel());
    Assert.assertEquals("Captur", info.getRawModel());
    Assert.assertEquals("RENAULT", info.getMark());
  }

  public void setMarkModelParser(MarkModelParser markModelParser) {
    this.markModelParser = markModelParser;
  }

  public void setUnunifiedFieldManager(UnunifiedFieldManager ununifiedFieldManager) {
    this.ununifiedFieldManager = ununifiedFieldManager;
  }
}
