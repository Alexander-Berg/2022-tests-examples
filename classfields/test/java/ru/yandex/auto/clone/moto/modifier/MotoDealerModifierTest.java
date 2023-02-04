package ru.yandex.auto.clone.moto.modifier;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.core.model.TypedAttribute;
import ru.yandex.auto.core.model.enums.State;
import ru.yandex.auto.core.model.moto.UnifiedMotoInfo;

@ContextConfiguration(locations = "/context/shard-test.xml")
@Ignore
public class MotoDealerModifierTest extends AbstractJUnit4SpringContextTests {

  @Autowired private MotoDealerModifier motoDealerModifier;

  @Test
  public void modify() {
    UnifiedMotoInfo unifiedMotoInfo = new UnifiedMotoInfo();
    unifiedMotoInfo.setMark(new TypedAttribute("mark", "HONDA", "HONDA", "HONDA"));
    unifiedMotoInfo.setModel(new TypedAttribute("model", "VFR_1200", "VFR_1200", "VFR_1200"));
    unifiedMotoInfo.setAutoruClientId("28312");
    unifiedMotoInfo.setStateKey(State.NEW.toString());

    motoDealerModifier.modify(unifiedMotoInfo);

    Assert.assertEquals(new Long(21271198L), unifiedMotoInfo.getVerbaDealerId());
  }
}
