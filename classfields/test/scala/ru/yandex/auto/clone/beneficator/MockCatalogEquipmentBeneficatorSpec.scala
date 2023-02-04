package ru.yandex.auto.clone.beneficator

import java.util.Collections
import org.junit.{Ignore, Test}
import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.core.catalog.grouping.{CatalogCardGroupingService, GroupByConfigurationImpl}
import ru.yandex.auto.core.catalog.model.ConfigurationImpl
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.vertis.mockito.MockitoSupport.{mock, when}

@ContextConfiguration(locations = Array("/context/shard-test.xml"))
@Ignore
class MockCatalogEquipmentBeneficatorSpec extends AbstractJUnit4SpringContextTests with Matchers {
  @Autowired var modifier: CatalogEquipmentBeneficator = _

  @Test // null seats for configuration
  def nullSeatsForConfiguration(): Unit = {
    val configuration = mock[ConfigurationImpl]
    when(configuration.getSeats).thenReturn(Array[String](null))

    val groupByConfiguration = mock[GroupByConfigurationImpl]
    when(groupByConfiguration.getKey).thenReturn(configuration)

    val catalogCardGroupingService: CatalogCardGroupingService = mock[CatalogCardGroupingService]
    when(catalogCardGroupingService.buildGroupByConfiguration(1L)).thenReturn(groupByConfiguration)
    modifier.setCatalogCardGroupingService(catalogCardGroupingService)

    val info = new UnifiedCarInfo("5")
    info.setMark("MERCEDES")
    info.setModel("S_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(1L)))
    info.setTechParamId(21016292L)
    info.setComplectationId(21016733L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-4"))
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 1
    Array("seats-4") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }
}
