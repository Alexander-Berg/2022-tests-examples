package ru.yandex.auto.indexer.lucene.handler.catalog.oxm.xstream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.auto.core.catalog.model.CatalogCardImpl;
import ru.yandex.auto.indexer.lucene.handler.catalog.oxm.xstream.builder.CatalogByVerbaServiceBuilder;
import ru.yandex.auto.message.CatalogSchema;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.functional.Filter;

/** User: yan1984 Date: 26.04.2011 16:01:17 */
@Ignore // fixme Error creating bean with name 'catalogByVerbaServiceBuilder' defined in class path
// resource [serializer.xml]: Cannot resolve reference to bean 'autoPremierePhotosBuilder'
// while setting bean property 'autoPremierePhotosBuilder'; nested exception is
// org.springframework.beans.factory.NoSuchBeanDefinitionException: No bean named
// 'autoPremierePhotosBuilder' is defined
public class CatalogCardsConverterImplTest extends AbstractConverterImplTests {

  private CatalogByVerbaServiceBuilder catalogByVerbaServiceBuilder;

  @Autowired
  public void setCatalogByVerbaServiceBuilder(
      CatalogByVerbaServiceBuilder catalogByVerbaServiceBuilder) {
    this.catalogByVerbaServiceBuilder = catalogByVerbaServiceBuilder;
  }

  @Test
  public void testUnmarshalling() throws Exception {
    InputStream is = new ClassPathResource("/auto-catalog.1.xml").getInputStream();

    List<CatalogCardImpl> cardList = super.unmarshall(catalogByVerbaServiceBuilder, is);

    CatalogCardImpl catalogCardWithComplect =
        CollectionUtils.firstOrNull(
            CollectionUtils.filter(
                cardList,
                new Filter<CatalogCardImpl>() {
                  @Override
                  public boolean fits(@NotNull CatalogCardImpl catalogCard) {
                    return catalogCard.getGeneration().getId() == 10000144l
                        && catalogCard.getTechParameter().getId() == 2306689l;
                  }
                }));
    Assert.assertNotNull(catalogCardWithComplect);
    Assert.assertNotNull(catalogCardWithComplect.getComplectation());
    Assert.assertEquals(570500, catalogCardWithComplect.getRawPriceForRussia(), 1);

    Assert.assertEquals("FORD", catalogCardWithComplect.getMark().getCode());
    Assert.assertEquals("RUR", catalogCardWithComplect.getComplectation().getCurrencyName());

    CatalogCardImpl catalogCardWithClosedGeneration =
        CollectionUtils.firstOrNull(
            CollectionUtils.filter(
                cardList,
                new Filter<CatalogCardImpl>() {
                  @Override
                  public boolean fits(@NotNull CatalogCardImpl catalogCard) {
                    return catalogCard.getGeneration().getId() == 10000144l;
                  }
                }));
    Assert.assertNotNull(catalogCardWithClosedGeneration);
    Assert.assertEquals(2010, (int) catalogCardWithClosedGeneration.getGeneration().getYearTo());

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream output = new GZIPOutputStream(bos);

    for (CatalogCardImpl card : cardList) {
      if (card.getLocales() != null && card.getLocales().length > 0) {
        CatalogSchema.CatalogCardMessage msg = card.toMessage();
        msg.writeDelimitedTo(output);
      }
    }

    output.close();

    System.out.println("bos.toByteArray().length = " + bos.toByteArray().length);
  }
}
