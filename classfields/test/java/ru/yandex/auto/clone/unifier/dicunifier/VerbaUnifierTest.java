package ru.yandex.auto.clone.unifier.dicunifier;

import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.auto.clone.model.UnifiedCarAccessor;
import ru.yandex.auto.core.dictionary.FieldDictionary;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.unifier.UnunifiedFieldManager;

@ContextConfiguration(locations = "/context/unifiers.xml")
@Ignore
public class VerbaUnifierTest extends AbstractJUnit4SpringContextTests {
  private final VerbaUnifier verbaUnifier = new VerbaUnifier();

  @Autowired private UnunifiedFieldManager ununifiedFieldManager;
  @Autowired private FieldDictionary fieldDictionary;

  @Before
  public void setUp() {
    verbaUnifier.setFieldDictionary(fieldDictionary);
    verbaUnifier.setFields(Lists.newArrayList(UnifiedCarAccessor.BODY_TYPE_FULL));
  }

  @Test
  public void testIt() {
    UnifiedCarInfo carInfo = new UnifiedCarInfo("1");
    carInfo.setBodyTypeFull("ALLROAD");
    carInfo.setNameplate("4092");
    verbaUnifier.unify(carInfo, ununifiedFieldManager);
  }
}
