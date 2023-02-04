package ru.yandex.auto.clone.trucks.modifier;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.model.TypedAttribute;
import ru.yandex.auto.core.model.enums.State;
import ru.yandex.auto.core.model.trucks.UnifiedTruckInfo;

@ContextConfiguration(locations = "/context/shard-test.xml")
@Ignore
public class TruckDealerModifierTest extends AbstractJUnit4SpringContextTests {

  @Autowired private TruckDealerModifier trucks;

  @Test
  public void gasDealerTest() {
    UnifiedTruckInfo uti = new UnifiedTruckInfo();
    uti.setMark(new TypedAttribute("mark", "GAZ", "GAZ", "GAZ"));
    uti.setModel(new TypedAttribute("model", "SOBOL_2752", "SOBOL_2752", "SOBOL_2752"));
    uti.setAutoruClientId("20113");
    uti.setAutoCathegory("TRUCK_CAT_LCV");
    uti.setStateKey(State.NEW.toString());
    uti.setCarLocation("Екатеринбург, ул.Маневровая, 45");

    trucks.modify(uti);

    Assert.assertEquals(new Long(21270652L), uti.getVerbaDealerId());
  }

  @Test
  public void gasDealerTestGAZ() {
    UnifiedTruckInfo uti = new UnifiedTruckInfo();
    uti.setMark(new TypedAttribute("mark", "GAZ", "GAZ", "GAZ"));
    uti.setModel(new TypedAttribute("model", "GAZEL_3302", "GAZEL_3302", "GAZEL_3302"));
    uti.setAutoruClientId("20113");
    uti.setAutoCathegory("TRUCK_CAT_LCV");
    uti.setStateKey(State.NEW.toString());
    uti.setCarLocation("Екатеринбург, ул.Маневровая, 45");

    trucks.modify(uti);

    Assert.assertEquals(new Long(21270652L), uti.getVerbaDealerId());
  }
}
