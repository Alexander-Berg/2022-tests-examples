package ru.yandex.vertis.moderation.service

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.SignalSet

/**
  * Specs on [[OwnerService]] implemented in-memory
  *
  * @author sunlight
  */
trait OwnerServiceSpecBase extends SpecBase {

  def getOwnerService: OwnerService

  "update" should {

    "correctly add new owner" in {
      val ownerService = getOwnerService

      val owner = OwnerGen.next
      ownerService.update(owner).futureValue
      val result = ownerService.get(owner.user).futureValue

      result should be(owner)
    }

    "correctly update old owner" in {
      val ownerService = getOwnerService

      val owner = OwnerGen.next
      ownerService.update(owner).futureValue
      val newOwner = ownerGen(owner.user).next
      ownerService.update(newOwner).futureValue

      val result = ownerService.get(owner.user).futureValue

      result should be(newOwner)
    }

    "correctly update old owner when adding switchOff" in {
      val ownerService = getOwnerService

      val signal = InheritedSignalGen.withoutSwitchOff.next

      val owner = OwnerGen.next
      ownerService.update(owner.copy(signals = owner.signals ++ SignalSet(signal))).futureValue

      val newOwner =
        owner.copy(signals = owner.signals ++ SignalSet(signal.withSwitchOff(Some(SignalSwitchOffGen.next))))
      ownerService.update(newOwner).futureValue

      val result = ownerService.get(owner.user).futureValue

      result should be(newOwner)
    }

    "correctly update old owner when deleting switchOff" in {
      val ownerService = getOwnerService

      val signal = InheritedSignalGen.withoutSwitchOff.next.withSwitchOff(Some(SignalSwitchOffGen.next))

      val owner = OwnerGen.next
      ownerService.update(owner.copy(signals = owner.signals ++ SignalSet(signal))).futureValue

      val newOwner = owner.copy(signals = owner.signals ++ SignalSet(signal.withSwitchOff(None)))
      ownerService.update(newOwner).futureValue

      val result = ownerService.get(owner.user).futureValue

      result should be(newOwner)
    }

    "correctly update the same signals" in {
      val ownerService = getOwnerService

      val owner = OwnerGen.next
      ownerService.update(owner).futureValue
      ownerService.update(owner).futureValue

      val result = ownerService.get(owner.user).futureValue

      result should be(owner)
    }
  }

  "get" should {
    "Owner with empty signals for unknown user" in {
      val ownerService = getOwnerService

      val user = UserGen.next
      val result = ownerService.get(user).futureValue
      result.signals.isEmpty should be(true)
    }
  }

}
