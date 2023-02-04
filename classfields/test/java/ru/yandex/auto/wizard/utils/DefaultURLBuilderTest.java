package ru.yandex.auto.wizard.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.auto.wizard.utils.urls.DefaultURLBuilder;

/** @author andkomarov */
public class DefaultURLBuilderTest {

  private DefaultURLBuilder builder;

  @Before
  public void setUp() throws Exception {
    builder = new DefaultURLBuilder("ya.ru");
  }

  @Test
  public void testBasic() {
    Assert.assertEquals("https://ya.ru/", builder.build());
  }

  @Test
  public void testPath() {
    builder.addPath("one", "two", "three");
    Assert.assertEquals("https://ya.ru/one/two/three/", builder.build());
  }

  @Test
  public void testParams() throws Exception {
    builder.addParam("key", "value");
    Assert.assertEquals("https://ya.ru/?key=value", builder.build());
  }

  @Test
  public void testSchema() throws Exception {
    builder.setSchema("http");
    Assert.assertEquals("http://ya.ru/", builder.build());
  }
}
