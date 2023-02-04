package ru.yandex.auto.clone.unifier.modifier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.core.model.cert.AutoruCertificate;
import ru.yandex.auto.core.model.enums.State;

public class TagsModifierTest {
  private TagsModifier modifier;
  private AutoruCertificate certificate;
  private final String type = "type";

  @Before
  public void setUp() {
    modifier = new TagsModifier();
    certificate = new AutoruCertificate("id", type, 42L);
  }

  @Test
  public void testUsedCar() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setAutoruCertificate(certificate);
    info.setState(State.EXCELLENT.toString());

    Assert.assertTrue(info.getSearchTags().isEmpty());
    modifier.modify(info);
    Assert.assertTrue(info.getSearchTags().contains(SearchTag.CERTIFICATE_MANUFACTURER.getValue()));
    Assert.assertTrue(info.getSearchTags().contains(SearchTag.CERTIFICATE.getValue() + "_" + type));
  }

  @Test
  public void testNewCar() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setAutoruCertificate(certificate);
    info.setState(State.NEW.toString());

    Assert.assertTrue(info.getSearchTags().isEmpty());
    modifier.modify(info);
    Assert.assertTrue(info.getSearchTags().contains(SearchTag.CERTIFICATE.getValue() + "_" + type));
  }

  @Test
  public void testBeatenCar() {
    UnifiedCarInfo info = new UnifiedCarInfo("1");
    info.setAutoruCertificate(certificate);
    info.setState(State.NEW.toString());

    Assert.assertTrue(info.getSearchTags().isEmpty());
    modifier.modify(info);
    Assert.assertTrue(info.getSearchTags().contains(SearchTag.CERTIFICATE.getValue() + "_" + type));
  }
}
