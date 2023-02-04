package ru.yandex.vertis.moderation.instance

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Essentials, Instance, TelephonesEssentials}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.proto.Model.TelephonesEssentials.PhoneOrigin

@RunWith(classOf[JUnitRunner])
class EssentialsPatchCalculatorSpec extends SpecBase {

  private def getNonEmptyInstance(service: Service): Gen[Instance] =
    instanceGen(service)
      .filter(_.essentials != Essentials.empty(service))

  private val telephoneEssentials = TelephonesEssentials(origin = Some(PhoneOrigin.TELEPONY))
  private val telephoneInstance = instanceGen(Service.TELEPHONES).next.copy(essentials = telephoneEssentials)
  private val emptyTelephoneInstance = telephoneInstance.copy(essentials = TelephonesEssentials.Empty)

  private val calculator = EssentialsPatchCalculatorImpl

  "EssentialsPatchCalculator" should {

    "create patch if essentials are different" in {
      val service = ServiceGen.next
      val instanceOriginal = getNonEmptyInstance(service).next
      val instanceUpdated = getNonEmptyInstance(service).filter(_.essentials != instanceOriginal.essentials).next

      val patch = calculator.calculatePatch(instanceOriginal, instanceUpdated)

      patch should not be empty
      patch.get.id shouldBe instanceOriginal.id
      patch.get.essentials shouldBe instanceUpdated.essentials
      patch.get.createTime shouldBe instanceOriginal.createTime
      patch.get.essentialsUpdateTime shouldBe instanceUpdated.essentialsUpdateTime
    }

    "not create patch if essentials are the same" in {
      val service = ServiceGen.next
      val instance = getNonEmptyInstance(service).next

      val patch = calculator.calculatePatch(instance, instance)

      patch shouldBe empty
    }

    "create technical essentials for telephones if essentials isn't present" in {
      val patch = calculator.calculatePatch(emptyTelephoneInstance, None)

      patch should not be empty
      patch.get.essentials shouldBe TelephonesEssentials.Technical
    }

    "not not create technical essentials if essentials isn't empty" in {
      val patch = calculator.calculatePatch(telephoneInstance, None)

      patch shouldBe empty
    }
  }
}
