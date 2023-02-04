package ru.yandex.vertis.general.common.dictionaries.testkit

import general.gost.palma.{BanReason, Text}
import ru.yandex.vertis.general.common.dictionaries.BansDictionaryService
import ru.yandex.vertis.general.common.dictionaries.BansDictionaryService.BansDictionaryService
import ru.yandex.vertis.general.common.resources.ban_reasons.BanReasonSnapshot
import zio.{UIO, ZIO, ZLayer}

object TestBansDictionaryService extends BansDictionaryService.Service {

  val editableReason: BanReason = BanReason(
    code = "Editable",
    text = Some(Text(main = "Editable ban")),
    offerEditable = BanReason.OfferBlockStatus.EDITABLE
  )

  val nonEditableReason: BanReason = BanReason(
    code = "NonEditable",
    text = Some(Text(main = "Non editable ban")),
    offerEditable = BanReason.OfferBlockStatus.NON_EDITABLE
  )

  val exclusionReason: BanReason = BanReason(
    code = "Exclusion",
    text = Some(Text(main = "Exclusion hide")),
    offerEditable = BanReason.OfferBlockStatus.EXCLUSION
  )

  val unknownReason: BanReason = BanReason(
    code = "Unknown",
    text = Some(Text(main = "Unknown reason")),
    offerEditable = BanReason.OfferBlockStatus.EDITABLE
  )

  val defaultUserBanReason: BanReason = BanReason(
    code = "test_ban_reason",
    titleLk = "Test Title",
    text = Some(
      Text(
        userBanHtml = "User ban html"
      )
    )
  )

  private val banReasonsSnapshot: BanReasonSnapshot =
    BanReasonSnapshot(Seq(editableReason, nonEditableReason, exclusionReason, defaultUserBanReason))

  override def banReasons: UIO[BanReasonSnapshot] = ZIO.succeed(banReasonsSnapshot)

  val service: ZIO[Any, Nothing, BansDictionaryService.Service] = ZIO.succeed(this)

  val layer: ZLayer[Any, Nothing, BansDictionaryService] = service.toLayer

  val emptyLayer: ZLayer[Any, Nothing, BansDictionaryService] = ZLayer.succeed {
    new BansDictionaryService.Service {
      override def banReasons: UIO[BanReasonSnapshot] = ZIO.succeed(BanReasonSnapshot.empty)
    }
  }
}
