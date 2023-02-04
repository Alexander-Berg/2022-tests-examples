package ru.yandex.vertis.hipe.pushes.dealer

import ru.auto.api.{ApiOfferModel, CommonModel}
import ru.yandex.pushnoy.PushRequestModel.DealerPhoneCallTemplate
import ru.yandex.vertis.hipe.clients.BaseSpec
import ru.yandex.vertis.hipe.clients.searcher.SearcherClient
import ru.yandex.vertis.hipe.clients.vos.VosClient
import ru.yandex.vertis.hipe.pushes.dealer.CallDealerBaseTest.CallDealerTest
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object CallDealerBaseTest {

  case class CallDealerTest(uuid: String,
                            offerId: String,
                            optTarget: Option[CallDealerBase.Target],
                            offer: ApiOfferModel.Offer)
    extends CallDealerBase {

    override def withTarget(newTarget: CallDealerBase.Target): CallDealerBase =
      copy(
        optTarget = Some(newTarget)
      )

    override def pushName: String = "test"

    override protected def getTargetOffer(
        searcherClient: SearcherClient,
        vosClient: VosClient
    )(implicit ec: ExecutionContext, trace: Traced): Future[(ApiOfferModel.Offer, DealerPhoneCallTemplate.TextType)] =
      Future.successful {
        (offer, DealerPhoneCallTemplate.TextType.DO_NOT_POSTPONE)
      }
  }
}

class CallDealerBaseTest extends BaseSpec with MockitoSupport {
  private val vosClient = mock[VosClient]
  private val searcherClient = mock[SearcherClient]
  implicit private val emptyTrace: Traced = Traced.empty

  "CallDealerBase" should {
    "use rouble price" in {
      val rurPrice = 2f

      val cd = CallDealerTest("test", "test", None, prepareOffer(rurPrice))
      val result = cd.withTarget(searcherClient, vosClient).await
      assert {
        result
          .flatMap {
            case c: CallDealerBase => c.optTarget.map(_.price)
            case _ => None
          }
          .exists(fixedPrecision(0.001)(_, rurPrice))
      }
    }
  }

  private def fixedPrecision(p: Double)(x: Double, y: Double): Boolean = {
    (x - y).abs <= p
  }

  private def prepareOffer(rurPrice: Float) = {
    val offer = ApiOfferModel.Offer
      .newBuilder()

    offer.getPriceInfoBuilder
      .setPrice(0.5f)
      .setRurPrice(rurPrice)
      .setEurPrice(0.5f)
      .setUsdPrice(0.7f)
    offer.getCarInfoBuilder
      .setMark("ZAZ")
      .setModel("966")

    offer.getStateBuilder
      .addImageUrls(CommonModel.Photo.newBuilder())

    offer.build
  }
}
