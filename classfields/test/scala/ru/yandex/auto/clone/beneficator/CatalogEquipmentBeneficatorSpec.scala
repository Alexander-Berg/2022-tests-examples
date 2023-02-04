package ru.yandex.auto.clone.beneficator

import java.util.Collections

import org.junit.{Ignore, Test}
import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.core.model.UnifiedCarInfo

@ContextConfiguration(locations = Array("/context/shard-test.xml"))
@Ignore
class CatalogEquipmentBeneficatorSpec extends AbstractJUnit4SpringContextTests with Matchers {
  @Autowired var modifier: CatalogEquipmentBeneficator = _

  @Test // 0-0-1
  def modifyAccordingToOneInCatalog(): Unit = {
    val info = new UnifiedCarInfo("1")
    info.setMark("GEELY")
    info.setModel("MK_CROSS")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(7767426L)))
    info.setTechParamId(7772446L)
    info.setGeneration("7767411")
    info.setComplectationId(7772447L)
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 0
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }

  @Test // 0-0-many
  def modifyAccordingToManyInCatalog(): Unit = {
    val info = new UnifiedCarInfo("2")
    info.setMark("KIA")
    info.setModel("SORENTO")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(4965854L)))
    info.setTechParamId(5012820L)
    info.setComplectationId(5014890L)
    modifier.modify(info)
    info.getSeats shouldBe "5"
    info.getEquipmentCodes.length shouldBe 0
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-5")
  }

  @Test // 0-1-1
  def modifyThirdRowAccordingToOneInCatalog(): Unit = {
    val info = new UnifiedCarInfo("3")
    info.setMark("MERCEDES")
    info.setModel("V_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(20109724L)))
    info.setTechParamId(21158254L)
    info.setGeneration("20109722")
    info.setComplectationId(21382559L)
    info.setEquipmentCodes(Array("third-row-seats"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 1
    Array("third-row-seats") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats")
  }

  @Test // 0-1-1 no seats>5
  def modifyThirdRowAccordingToOneInCatalogNoLarge5(): Unit = {
    val info = new UnifiedCarInfo("1")
    info.setMark("GEELY")
    info.setModel("MK_CROSS")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(7767426L)))
    info.setTechParamId(7772446L)
    info.setGeneration("7767411")
    info.setComplectationId(7772447L)
    info.setEquipmentCodes(Array("third-row-seats"))
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 0
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }

  @Test // 0-1-many
  def modifyThirdRowAccordingToManyInCatalog(): Unit = {
    val info = new UnifiedCarInfo("4")
    info.setMark("VOLKSWAGEN")
    info.setModel("CARAVELLE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(21793480L)))
    info.setTechParamId(21793516L)
    info.setComplectationId(21793909L)
    info.setEquipmentCodes(Array("third-row-seats"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 1
    Array("third-row-seats") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats")
  }

  @Test // 0-1-many  no seats>5
  def modifyThirdRowAccordingToManyInCatalogNoLarge5(): Unit = {
    val info = new UnifiedCarInfo("5")
    info.setMark("MERCEDES")
    info.setModel("S_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(21016168L)))
    info.setTechParamId(21016292L)
    info.setComplectationId(21016733L)
    info.setEquipmentCodes(Array("third-row-seats"))
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 0
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }

  @Test // 1-0-1 equals
  def modifyOfferWithOneInCatalogEquals(): Unit = {
    val info = new UnifiedCarInfo("1")
    info.setMark("GEELY")
    info.setModel("MK_CROSS")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(7767426L)))
    info.setTechParamId(7772446L)
    info.setGeneration("7767411")
    info.setComplectationId(7772447L)
    info.setEquipmentCodes(Array("seats-4"))
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 0
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }

  @Test // 1-0-1 not equals - we trust to offer
  def modifyOfferWithOneInCatalogNotEquals(): Unit = {
    val info = new UnifiedCarInfo("1")
    info.setMark("GEELY")
    info.setModel("MK_CROSS")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(7767426L)))
    info.setTechParamId(7772446L)
    info.setGeneration("7767411")
    info.setComplectationId(7772447L)
    info.setEquipmentCodes(Array("seats-5"))
    modifier.modify(info)
    info.getSeats shouldBe "5"
    info.getEquipmentCodes.length shouldBe 1
    Array("seats-5") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-5")
  }

  @Test // 1-0-many
  def modifyOfferWithManyInCatalog(): Unit = {
    val info = new UnifiedCarInfo("2")
    info.setMark("KIA")
    info.setModel("SORENTO")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(4965854L)))
    info.setTechParamId(5012820L)
    info.setComplectationId(5014890L)
    info.setEquipmentCodes(Array("seats-5"))
    modifier.modify(info)
    info.getSeats shouldBe "5"
    info.getEquipmentCodes.length shouldBe 1
    Array("seats-5") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-5")
  }

  @Test // 1-1-1 equals
  def modifyOfferWithThirdRowAccordingToOneInCatalogEquals(): Unit = {
    val info = new UnifiedCarInfo("3")
    info.setMark("MERCEDES")
    info.setModel("V_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(20109724L)))
    info.setTechParamId(21158254L)
    info.setGeneration("20109722")
    info.setComplectationId(21382559L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-6"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 1
    Array("third-row-seats") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats")
  }

  @Test // 1-1-1 not equals
  def modifyOfferWithThirdRowAccordingToOneInCatalogNotEquals(): Unit = {
    val info = new UnifiedCarInfo("3")
    info.setMark("MERCEDES")
    info.setModel("V_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(20109724L)))
    info.setTechParamId(21158254L)
    info.setGeneration("20109722")
    info.setComplectationId(21382559L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-7"))
    modifier.modify(info)
    info.getSeats shouldBe "7"
    info.getEquipmentCodes.length shouldBe 2
    Array("third-row-seats", "seats-7") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("third-row-seats", "seats-7")
  }

  @Test // 1-1-1 wrong for third row
  def modifyWrongOfferWithThirdRowAccordingToOneInCatalog(): Unit = {
    val info = new UnifiedCarInfo("3")
    info.setMark("MERCEDES")
    info.setModel("V_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(20109724L)))
    info.setTechParamId(21158254L)
    info.setGeneration("20109722")
    info.setComplectationId(21382559L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-5"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 1
    Array("third-row-seats") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats")
  }

  @Test // 1-1-many
  def modifyOfferWithThirdRowAccordingToManyInCatalog(): Unit = {
    val info = new UnifiedCarInfo("4")
    info.setMark("VOLKSWAGEN")
    info.setModel("CARAVELLE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(21793480L)))
    info.setTechParamId(21793516L)
    info.setComplectationId(21793909L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-6"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 2
    Array("seats-6", "third-row-seats") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 2
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats")
  }

  @Test // 1-1-many wrong for third row
  def modifyWrongOfferWithThirdRowAccordingToManyInCatalog(): Unit = {
    val info = new UnifiedCarInfo("4")
    info.setMark("VOLKSWAGEN")
    info.setModel("CARAVELLE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(21793480L)))
    info.setTechParamId(21793516L)
    info.setComplectationId(21793909L)
    info.setEquipmentCodes(Array("third-row-seats", "seats-5", "dha"))
    modifier.modify(info)
    info.getSeats shouldBe "6"
    info.getEquipmentCodes.length shouldBe 2
    Array("third-row-seats", "dha") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 3
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-6", "third-row-seats", "dha")
  }

  @Test // 1-1-many  no seats>5
  def modifyOfferThirdRowAccordingToManyInCatalogNoLarge5(): Unit = {
    val info = new UnifiedCarInfo("5")
    info.setMark("MERCEDES")
    info.setModel("S_KLASSE")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(21016168L)))
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

  @Test // fix npe error
  def seatHeatEquipmentOption(): Unit = {
    val info = new UnifiedCarInfo("1")
    info.setMark("GEELY")
    info.setModel("MK_CROSS")
    info.setConfigurationIdsMap(Collections.singletonMap("ru", Array(7767426L)))
    info.setTechParamId(7772446L)
    info.setGeneration("7767411")
    info.setComplectationId(7772447L)
    info.setEquipmentCodes(Array("seats-heat"))
    modifier.modify(info)
    info.getSeats shouldBe "4"
    info.getEquipmentCodes.length shouldBe 1
    Array("seats-heat") shouldBe (info.getEquipmentCodes)
    info.getSearchCatalogEquipmentCodes.length shouldBe 1
    info.getSearchCatalogEquipmentCodes shouldBe Array("seats-4")
  }
}
