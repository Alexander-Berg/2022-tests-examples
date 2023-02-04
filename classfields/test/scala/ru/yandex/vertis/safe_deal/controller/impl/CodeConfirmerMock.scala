package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.vertis.safe_deal.controller.CodeConfirmer
import ru.yandex.vertis.safe_deal.model.{DealId, DealParty}
import ru.yandex.vertis.safe_deal.proto.common.DealStep
import ru.yandex.vertis.zio_baker.model.UserId
import zio.Task

import java.time.LocalTime

class CodeConfirmerMock extends CodeConfirmer {

  override def requestConfirmationCode(
      id: DealId,
      userId: UserId,
      confirmationOpt: Option[DealParty.ConfirmationCode],
      step: DealStep,
      userStep: String): Task[DealParty.ConfirmationCode] =
    Task(confirmationOpt.getOrElse(DealParty.ConfirmationCode(LocalTime.now(), "111111", false)))

  override def checkConfirmationCode(
      id: DealId,
      userId: UserId,
      confirmationOpt: Option[DealParty.ConfirmationCode],
      code: String,
      step: DealStep,
      userStep: String): Task[DealParty.ConfirmationCode] =
    Task(
      confirmationOpt
        .map(_.copy(isConfirmed = true))
        .getOrElse(DealParty.ConfirmationCode(LocalTime.now(), "111111", true))
    )

}
