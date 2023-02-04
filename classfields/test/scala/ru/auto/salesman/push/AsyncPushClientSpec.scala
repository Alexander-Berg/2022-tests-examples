package ru.auto.salesman.push

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import ru.auto.salesman.model.{AutoRuOfferId, OfferCategories}
import ru.auto.salesman.push.impl.AsyncPushClientImpl
import ru.auto.salesman.test.model.gens.BillingModelGenerators.offerBillingGen
import ru.auto.salesman.test.{BaseSpec, TestAkkaComponents}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.protobuf.ProtobufUtils

class AsyncPushClientSpec extends BaseSpec with TestAkkaComponents {

  private val serverAddress = runServer {
    (put & path(
      "api" / "v2" / "cars" / "billing" / "partners" / "1438536" / "billing" / "autoru-1058223344"
    ) & entity(as[Array[Byte]])) { body =>
      val parsed =
        ProtobufUtils.parseDelimited(
          Model.OfferBilling.getDefaultInstance,
          body
        )

      if (parsed == Seq(offerBilling))
        complete(StatusCodes.OK)
      else
        complete(StatusCodes.BadRequest)
    } ~ (delete & path(
      "api" / "v2" / "cars" / "billing" / "partners" / "1438536" / "billing" / "autoru-1058223344"
    )) {
      complete(StatusCodes.OK)
    }
  }

  private val resolver = DefaultPushPathResolver(serverAddress.toString)
  private val client = new AsyncPushClientImpl(resolver)
  private val offerId = 1058223344
  private val offerBilling = offerBillingGen.sample.get

  "AsyncPushClientSpec" should {
    "set billings" in {
      client
        .set(
          AutoRuOfferId(offerId, OfferCategories.Cars),
          new AsyncPushClient.OfferBilling(offerBilling.toByteArray)
        )
        .futureValue shouldBe unit
    }

    "skip billing" in {
      client
        .skip(AutoRuOfferId(offerId, OfferCategories.Cars))
        .futureValue shouldBe unit
    }
  }
}
