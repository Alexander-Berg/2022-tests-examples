package ru.auto.salesman.push

import org.scalacheck.Gen
import ru.auto.salesman.push.AsyncPushClient.OfferBilling
import ru.auto.salesman.test.model.gens.BillingModelGenerators.offerBillingGen
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.protobuf.ProtobufUtils

class HttpEntityConverterSpec extends BaseSpec {
  "ProtoEntityConverter" should {
    "return same proto entity" in {
      forAll(Gen.listOf(offerBillingGen)) { offerBillings =>
        val entity = ProtoHttpEntityConverter.getEntity(
          offerBillings.map(_.toByteArray).map(new OfferBilling(_))
        )

        val parsed =
          ProtobufUtils.parseDelimited(
            Model.OfferBilling.getDefaultInstance,
            entity.data.toArray
          )

        parsed.toList shouldBe offerBillings

      }
    }
  }
}
