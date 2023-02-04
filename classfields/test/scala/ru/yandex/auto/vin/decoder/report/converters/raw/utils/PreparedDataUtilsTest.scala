package ru.yandex.auto.vin.decoder.report.converters.raw.utils

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.yandex.auto.vin.decoder.model.AudatexDealers
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.proto.VinHistory.{AdaperioAudatex, VinInfoHistory}
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.history.entities._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

class PreparedDataUtilsTest extends AnyFunSuite with MockitoSupport {

  private val historyEntities = List(
    AudatexPreparedReportHistoryEntity(
      1,
      AdaperioAudatex.Report.getDefaultInstance,
      None,
      isRed = false,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      "",
      Seq.empty,
      None
    ),
    AudatexPreparedCheckHistoryEntity(1)
  )

  private val prepared = Prepared(0, 0, 0, VinInfoHistory.getDefaultInstance, "")

  test("Dealer is not authorized for Audatex (enabled LOCKED status)") {
    val block =
      PreparedDataUtils.buildAudatex(
        isUpdating = false,
        Some(123L),
        Some("BMW"),
        Some(prepared),
        historyEntities,
        AudatexDealers(List.empty[AudatexPartner]),
        feature(true)
      )
    assert(block.data.get.status == Status.LOCKED)
    assert(block.data.get.entities.isEmpty)
    assert(!block.data.get.isUpdating)
  }

  test("Block is not updating and there are no YDB records") {
    val block =
      PreparedDataUtils.buildAudatex(
        isUpdating = false,
        Some(123L),
        Some("BMW"),
        None,
        List.empty,
        AudatexDealers(List.empty[AudatexPartner]),
        feature(true)
      )
    assert(block.data.isEmpty)
  }

  test("Dealer is not authorized for Audatex (disabled LOCKED status)") {
    val block =
      PreparedDataUtils.buildAudatex(
        isUpdating = false,
        Some(123L),
        Some("BMW"),
        Some(prepared),
        historyEntities,
        AudatexDealers(List.empty[AudatexPartner]),
        feature(false)
      )
    assert(block.data.isEmpty)
  }

//  test("Dealer is authorized for Audatex but mark is not supported") {
//    val block =
//      PreparedDataUtils.buildAudatex(
//        isUpdating = false,
//        Some(262L),
//        Some("VAZ"),
//        Some(prepared),
//        historyEntities,
//        AudatexDealers(List.empty[AudatexPartner]),
//        feature(true)
//      )
//    assertTrue(block.data.isEmpty)
//  }

  test("Dealer is authorized for Audatex and data is updating") {
    val block =
      PreparedDataUtils.buildAudatex(
        isUpdating = true,
        Some(262L),
        Some("BMW"),
        Some(prepared),
        historyEntities,
        AudatexDealers(List(AudatexPartner(Set(262L), "", Audatex.Credentials("", "")))),
        feature(true)
      )
    assert(block.data.get.status == Status.IN_PROGRESS)
    assert(block.data.get.entities.length == 1)
    assert(block.data.get.isUpdating)
  }

//  test("Non-dealer requests Audatex with unsupported mark") {
//    val block = PreparedDataUtils.buildAudatex(
//      isUpdating = false,
//      None,
//      Some("VAZ"),
//      Some(prepared),
//      historyEntities,
//      AudatexDealers(List.empty[AudatexPartner]),
//      feature(true)
//    )
//    assertTrue(block.data.isEmpty)
//  }

  test("Non-dealer requests Audatex") {
    val block = PreparedDataUtils.buildAudatex(
      isUpdating = false,
      None,
      Some("BMW"),
      Some(prepared),
      historyEntities,
      AudatexDealers(List.empty[AudatexPartner]),
      feature(true)
    )
    assert(block.data.get.status == Status.ERROR)
    assert(block.data.get.entities.length == 1)
    assert(!block.data.get.isUpdating)
  }

  test("Non-dealer requests Audatex but data is updating") {
    val block =
      PreparedDataUtils.buildAudatex(
        isUpdating = true,
        None,
        None,
        Some(prepared),
        historyEntities,
        AudatexDealers(List.empty[AudatexPartner]),
        feature(true)
      )
    assert(block.data.get.status == Status.IN_PROGRESS)
    assert(block.data.get.entities.length == 1)
    assert(block.data.get.isUpdating)
  }

  def feature(value: Boolean): Feature[Boolean] = {
    val feature = mock[Feature[Boolean]]
    when(feature.value).thenReturn(value)
    feature
  }
}
