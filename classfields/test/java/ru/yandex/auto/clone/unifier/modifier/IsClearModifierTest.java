package ru.yandex.auto.clone.unifier.modifier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.core.model.UnifiedCarInfo;

/** @author andkomarov */
public class IsClearModifierTest {
  private IsClearModifier modifier;

  @Before
  public void setUp() {
    modifier = new IsClearModifier();
  }

  @Test
  public void testClear() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setWanted(true);
    info.setProhibtion(true);
    info.setAccident(0);

    Assert.assertNull(info.getIsClear());
    modifier.modify(info);
    Assert.assertTrue(info.getIsClear());
  }

  @Test
  public void testClear2() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setWanted(true);

    Assert.assertNull(info.getIsClear());
    modifier.modify(info);
    Assert.assertTrue(info.getIsClear());
  }

  @Test
  public void testNotClear() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");

    modifier.modify(info);
    Assert.assertNull(info.getIsClear());

    info.setWanted(true);
    info.setProhibtion(true);
    info.setAccident(1);
    modifier.modify(info);
    Assert.assertTrue(info.getIsClear());
  }

  @Test
  public void testNoClear2() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setProhibtion(false);

    Assert.assertNull(info.getIsClear());
    modifier.modify(info);
    Assert.assertFalse(info.getIsClear());
  }

  @Test
  public void testNull() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    modifier.modify(info);
    Assert.assertNull(info.getIsClear());
  }
}
