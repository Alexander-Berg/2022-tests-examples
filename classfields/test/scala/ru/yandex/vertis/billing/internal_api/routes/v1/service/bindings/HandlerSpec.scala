package ru.yandex.vertis.billing.internal_api.routes.v1.service.bindings

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.internal_api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.v1.service.campaigns.Marshallers
import ru.yandex.vertis.billing.model_core.gens.{OfferBillingGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.internal_api.routes.v1.service.bindings.Handler
import ru.yandex.vertis.billing.util.CacheControl

import scala.concurrent.Future

/**
  * Specs on offer billings handler [[Handler]]
  *
  * @author alesavin
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/offer-billings"

  implicit val offerBillingsUnmarshaller: FromEntityUnmarshaller[Seq[Model.OfferBilling]] =
    Marshallers.itUnmarshaller(Model.OfferBilling.getDefaultInstance)

  "GET /" should {
    "provide bindings" in {
      val bindings = OfferBillingGen.next(2).map(Conversions.toMessage)
      stub(backend.offerBillingStorage.get.get(_: CacheControl)) { case CacheControl.Cache =>
        Future.successful(bindings)
      }
      Get(url("/")) ~> route ~> check {
        status should be(StatusCodes.OK)
        val result = responseAs[Seq[Model.OfferBilling]]
        result should contain theSameElementsAs bindings
      }
    }
  }
}
