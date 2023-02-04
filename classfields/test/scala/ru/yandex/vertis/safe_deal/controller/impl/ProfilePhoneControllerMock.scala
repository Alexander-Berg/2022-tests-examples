package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.vertis.safe_deal.controller.ProfilePhoneController
import ru.yandex.vertis.safe_deal.model.{Deal, Entity, PersonProfile}
import zio.Task

class ProfilePhoneControllerMock() extends ProfilePhoneController.Service {

  override def isBuyerPhoneConfirmed(deal: Deal, newPhone: Option[Entity.PhoneEntity]): Task[Boolean] =
    Task.succeed(true)

  override def isSellerPhoneConfirmed(deal: Deal, newPhone: Option[Entity.PhoneEntity]): Task[Boolean] =
    Task.succeed(true)

  override def isConfirmedPhoneChanged(
      personProfile: Option[PersonProfile],
      newPhoneEntity: Option[Entity.PhoneEntity]): Boolean = false
}
