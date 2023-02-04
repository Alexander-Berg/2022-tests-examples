package carfax.consumers

import auto.carfax.common.clients.passport.PassportClient
import auto.carfax.common.clients.vos.VosClient
import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AsyncFunSuite
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.CarsModel.CarInfo
import ru.auto.salesman.model.user.ApiModel.{VinHistoryBoughtReport, VinHistoryBoughtReports}
import ru.yandex.auto.vin.decoder.model.AutoruUser
import ru.yandex.auto.vin.decoder.pushnoy.PushnoyManager
import ru.yandex.auto.vin.decoder.salesman.SalesmanClient
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.telepony.model.proto.TeleponyCall

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IterableHasAsJava

class KafkaCallsProcessorTest extends AsyncFunSuite with BeforeAndAfter with MockitoSupport {
  implicit val m = TestOperationalSupport
  implicit val t = Traced.empty

  val passportClient = mock[PassportClient]
  val salesmanClient = mock[SalesmanClient]
  val pushnoyManager = mock[PushnoyManager]
  val vosClient = mock[VosClient]

  val processor = new KafkaCallsProcessor(passportClient, salesmanClient, vosClient, pushnoyManager)

  before {
    reset(passportClient)
    reset(salesmanClient)
    reset(pushnoyManager)
    reset(vosClient)
  }

  val idWithHash = "1097214678-gs231af"
  val idWithHashResponse: Future[Some[String]] = Future.successful(Some(idWithHash))

  def offerBuilder: Offer.Builder = Offer
    .newBuilder()
    .setUrl("")
    .setCategory(ApiOfferModel.Category.CARS)
    .setId(idWithHash)
    .setCarInfo(CarInfo.newBuilder().setMark("Mercedes").setModel("Benz"))

  val offerWithIncorrectCategory: Future[Some[Offer]] = Future.successful(
    Some(
      offerBuilder
        .setCategory(ApiOfferModel.Category.CATEGORY_UNKNOWN)
        .build()
    )
  )

  val offer: Future[Some[Offer]] = {
    Future.successful(
      Some(
        offerBuilder.build()
      )
    )
  }

  def makeCall(talkDurationSeconds: Int): TeleponyCall = {
    TeleponyCall
      .newBuilder()
      .setObjectId("1097214678")
      .setDomain("autoru_def")
      .setTalkDurationSeconds(talkDurationSeconds)
      .build()
  }

  test("send push") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(AutoruUser(1))))
    when(vosClient.getAutoRuIdWithHash(?, ?)(?)).thenReturn(idWithHashResponse)
    when(vosClient.getOffer(?)(?)).thenReturn(offer)
    when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
      .thenReturn(Future.successful(VinHistoryBoughtReports.newBuilder().build()))
    when(pushnoyManager.sendPushes(?, ?, ?)(?)).thenReturn(Future.successful(1))

    processor.processCall(makeCall(60)).map { _ =>
      verify(pushnoyManager).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("dont send push cuz short call") {
    processor.processCall(makeCall(29)).map { _ =>
      verify(pushnoyManager, never()).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("dont send push cuz no user") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(None))

    processor.processCall(makeCall(60)).map { _ =>
      verify(pushnoyManager, never()).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("dont send push cuz no offer") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(AutoruUser(1))))
    when(vosClient.getAutoRuIdWithHash(?, ?)(?)).thenReturn(idWithHashResponse)
    when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))
    processor.processCall(makeCall(60)).map { _ =>
      verify(vosClient).getOffer(?)(?)
      verify(pushnoyManager, never()).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("dont send push cuz has purchases") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(AutoruUser(1))))
    when(vosClient.getOffer(?)(?)).thenReturn(offer)
    when(vosClient.getAutoRuIdWithHash(?, ?)(?)).thenReturn(idWithHashResponse)
    when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
      .thenReturn(
        Future.successful(
          VinHistoryBoughtReports
            .newBuilder()
            .addAllReports(Seq(VinHistoryBoughtReport.newBuilder().setOfferId("").build()).asJava)
            .build()
        )
      )

    processor.processCall(makeCall(60)).map { _ =>
      verify(salesmanClient).getBoughtReports(?, ?, ?, ?)(?)
      verify(pushnoyManager, never()).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("dont send push cuz no offer id with hash") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(AutoruUser(1))))
    when(vosClient.getAutoRuIdWithHash(?, ?)(?)).thenReturn(Future.successful(None))
    processor.processCall(makeCall(60)).map { _ =>
      verify(vosClient, never()).getOffer(?)(?)
      verify(pushnoyManager, never()).sendPushes(?, ?, ?)(?)
      succeed
    }
  }

  test("throw illegalArg on incorrect offer category") {
    when(passportClient.userSearch(?)(?)).thenReturn(Future.successful(Some(AutoruUser(1))))
    when(vosClient.getAutoRuIdWithHash(?, ?)(?)).thenReturn(idWithHashResponse)
    when(vosClient.getOffer(?)(?)).thenReturn(offerWithIncorrectCategory)
    when(salesmanClient.getBoughtReports(?, ?, ?, ?)(?))
      .thenReturn(
        Future.successful(
          VinHistoryBoughtReports
            .newBuilder()
            .build()
        )
      )
    recoverToSucceededIf[IllegalArgumentException] {
      processor.processCall(makeCall(60))
    }
  }
}
