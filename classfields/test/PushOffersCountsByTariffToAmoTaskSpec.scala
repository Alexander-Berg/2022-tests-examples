package auto.dealers.amoyak.tasks

import com.typesafe.config.Config
import auto.common.clients.vos.Vos
import auto.common.clients.vos.Vos.OwnerId.DealerId
import auto.common.clients.vos.Vos.{SectionFilter, StatusFilter}
import auto.dealers.amoyak.storage.testkit.{AmoIntegratorClientMock, ClientsChangedBufferDaoMock}
import auto.common.clients.vos.testkit.VosTest
import common.zio.clock.MoscowClock
import common.zio.pureconfig.Pureconfig
import auto.dealers.amoyak.model.AmoCrmPatch.OffersPatch
import auto.dealers.amoyak.model.{AmoMessageType, OfferCountByTariff}
import auto.dealers.amoyak.model.Tariff.{CarsNew, CarsUsed, Commercial, Moto}
import auto.dealers.amoyak.storage.dao.ClientsChangedBufferDao.ResultRecord
import ru.auto.api.api_offer_model.Category
import ru.auto.api.api_offer_model.Category.{CARS, MOTO, TRUCKS}
import ru.auto.api.api_offer_model.OfferStatus.{ACTIVE, BANNED, EXPIRED, INACTIVE}
import ru.auto.api.api_offer_model.Section.{NEW, USED}
import common.zio.logging.Logging
import zio.{Has, ZLayer}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.{Expectation, MockClock}
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, _}

import java.time.ZonedDateTime

object PushOffersCountsByTariffToAmoTaskSpec extends DefaultRunnableSpec {

  object testState {

    val timePoint =
      ZonedDateTime.of(2020, 1, 8, 0, 0, 0, 0, MoscowClock.timeZone)

    val task = new PushOffersCountsByTariffToAmoTask()

    val clientsChangedBufferState = Seq(
      ResultRecord(0, 0, "vos")
    )

    val vosState: List[(Option[Category], Vos.OwnerId, List[Vos.OffersCountFilter])] =
      List(
        (Some(CARS), DealerId(0), List(StatusFilter(ACTIVE), SectionFilter(NEW))),
        (Some(CARS), DealerId(0), List(StatusFilter(ACTIVE), SectionFilter(USED))),
        (Some(TRUCKS), DealerId(0), List(StatusFilter(ACTIVE))),
        (Some(MOTO), DealerId(0), List(StatusFilter(ACTIVE))),
        (Some(CARS), DealerId(0), List(StatusFilter(INACTIVE), SectionFilter(NEW))),
        (Some(CARS), DealerId(0), List(StatusFilter(INACTIVE), SectionFilter(USED))),
        (Some(TRUCKS), DealerId(0), List(StatusFilter(INACTIVE))),
        (Some(MOTO), DealerId(0), List(StatusFilter(INACTIVE))),
        (Some(CARS), DealerId(0), List(StatusFilter(BANNED), SectionFilter(NEW))),
        (Some(CARS), DealerId(0), List(StatusFilter(BANNED), SectionFilter(USED))),
        (Some(TRUCKS), DealerId(0), List(StatusFilter(BANNED))),
        (Some(MOTO), DealerId(0), List(StatusFilter(BANNED))),
        (Some(CARS), DealerId(0), List(StatusFilter(EXPIRED), SectionFilter(NEW))),
        (Some(CARS), DealerId(0), List(StatusFilter(EXPIRED), SectionFilter(USED))),
        (Some(TRUCKS), DealerId(0), List(StatusFilter(EXPIRED))),
        (Some(MOTO), DealerId(0), List(StatusFilter(EXPIRED)))
      )

  }

  val pushOffersCountsByTariffToAmoTest = testM("Single full PushOffersCountsByTariffToAmoTask run") {
    import testState._

    val expectedOfferCount: Seq[OfferCountByTariff] = Seq(
      OfferCountByTariff(CarsUsed, 1, 2, 1),
      OfferCountByTariff(CarsNew, 1, 2, 1),
      OfferCountByTariff(Commercial, 1, 2, 1),
      OfferCountByTariff(Moto, 1, 2, 1)
    )
    val expectedResult: AmoMessageType = OffersPatch(0L, expectedOfferCount, timePoint)

    val vos = VosTest.GetOffersCount(isOneOf(vosState), valueF(el => vosState.count(el == _)))

    val clock = MockClock.CurrentDateTime {
      value(timePoint.toOffsetDateTime)
    }.optional

    val amoClient =
      AmoIntegratorClientMock.PushMessage.of[AmoMessageType](equalTo(expectedResult), unit)

    val bufferGetData =
      ClientsChangedBufferDaoMock.GetByDataSource(equalTo("vos"), value(clientsChangedBufferState))

    val bufferDelete =
      ClientsChangedBufferDaoMock.Delete(equalTo(0L), unit)

    val env = bufferGetData ++ clock ++ vos.atMost(16) ++ amoClient ++ bufferDelete

    assertM(task.program)(isUnit).provideCustomLayer(env.toLayer ++ Logging.live)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PushOffersCountsByTariffToAmoTask")(
      pushOffersCountsByTariffToAmoTest
    )

}
