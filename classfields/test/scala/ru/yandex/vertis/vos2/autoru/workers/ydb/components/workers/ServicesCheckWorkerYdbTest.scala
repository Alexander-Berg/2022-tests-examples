package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.common.monitoring.error.ErrorReservoir
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.ServicesCheckWorkerYdb.{CheckIntervalMillis, Service}
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.utils._
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.salesman.SalesmanClient.Goods
import ru.yandex.vos2.autoru.services.salesman.{SalesmanClient, SalesmanUserClient}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Success

class ServicesCheckWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {
  val Now = new DateTime().getMillis // millis
  private case class Notification(actual: List[Service],
                                  expected: List[Service],
                                  notAppliedProducts: List[Service],
                                  wrongAppliedProducts: List[Service])
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val offer: Offer

    val salesmanClient = mock[SalesmanClient]
    val salesmanUserClient = mock[SalesmanUserClient]
    val notification = new AtomicReference[Notification]()
    val reservoirMock = mock[ErrorReservoir]

    val worker = new ServicesCheckWorkerYdb(
      salesmanClient: SalesmanClient,
      salesmanUserClient: SalesmanUserClient
    ) with YdbWorkerTestImpl {
      override protected val reservoir: ErrorReservoir = reservoirMock

      override protected def now(): Long = Now
      implicit override def ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(5))
    }
  }

  private def longToDateTime(timestamp: Long): ZonedDateTime =
    ZonedDateTime.of(
      LocalDateTime.ofEpochSecond(timestamp / 1000, (timestamp % 1000).toInt, ZoneOffset.UTC),
      ZoneOffset.UTC
    )

  "should ok" in new Fixture {
    when(salesmanClient.getGoods(?, ?)(?)).thenReturn(
      Success(
        Seq(
          Goods(
            1L,
            "cars",
            ServiceType.COLOR.name(),
            None,
            Some(longToDateTime(Now - 1.hour.toMillis)),
            Some(longToDateTime(Now + 1.hour.toMillis))
          )
        )
      )
    )
    doNothing().when(reservoirMock).ok()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .clearServices()
      .clearLastServicesCheckTimestamp()
      .addServices(PaidService.newBuilder().setServiceType(ServiceType.COLOR).setCreated(10000000).setIsActive(true))
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    getNextCheckState((res.nextState)).nextCheck.getMillis shouldBe (Now + CheckIntervalMillis) +- 10000

    verify(reservoirMock).ok()
    notification.get() shouldBe null
  }

  "should check different badges as different products" in new Fixture {
    pending
    when(salesmanClient.getGoods(?, ?)).thenReturn(
      Success(
        Seq(
          Goods(
            1L,
            "cars",
            ServiceType.COLOR.name(),
            None,
            Some(longToDateTime(Now - 1.hour.toMillis)),
            Some(longToDateTime(Now + 1.hour.toMillis))
          ),
          Goods(
            1L,
            "cars",
            ServiceType.BADGE.name(),
            Some("foo"),
            Some(longToDateTime(Now - 1.hour.toMillis)),
            Some(longToDateTime(Now + 1.hour.toMillis))
          )
        )
      )
    )
    doNothing().when(reservoirMock).error(ArgumentMatchers.any[String]())

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .clearServices()
      .clearLastServicesCheckTimestamp()
      .addServices(PaidService.newBuilder().setServiceType(ServiceType.COLOR).setCreated(Now).setIsActive(true))
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.BADGE)
          .setCreated(Now)
          .setIsActive(true)
          .setBadge("bar")
      )
    val offer = builder.build()
    val res = worker.process(offer, None).updateOfferFunc.get(offer)
    res.getOfferAutoru.getLastServicesCheckTimestamp shouldBe Now +- 10000

    notification.get().notAppliedProducts match {
      case Service(ServiceType.BADGE, Some("foo"), _, _) :: Nil =>
    }
    notification.get().wrongAppliedProducts match {
      case Service(ServiceType.BADGE, Some("bar"), _, _) :: Nil =>
    }
  }

  "don't notify about too fresh services" in new Fixture {
    when(salesmanClient.getGoods(?, ?)(?)).thenReturn(
      Success(
        Seq(
          Goods(
            1L,
            "cars",
            ServiceType.COLOR.name(),
            None,
            Some(longToDateTime(Now)),
            Some(longToDateTime(Now + 1.hour.toMillis))
          )
        )
      )
    )

    doNothing().when(reservoirMock).ok()

    val builder = TestUtils.createOffer(dealer = true)

    builder.getOfferAutoruBuilder.clearServices().clearLastServicesCheckTimestamp()
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    getNextCheckState((res.nextState)).nextCheck.getMillis shouldBe (Now + CheckIntervalMillis) +- 10000

    verify(reservoirMock).ok()
    notification.get() shouldBe null
  }

  "don't notify about already expired expected services" in new Fixture {
    when(salesmanClient.getGoods(?, ?)(?)).thenReturn(
      Success(
        Seq(
          Goods(
            1L,
            "cars",
            ServiceType.COLOR.name(),
            None,
            Some(longToDateTime(Now - 2.hour.toMillis)),
            Some(longToDateTime(Now - 1.hour.toMillis))
          )
        )
      )
    )

    doNothing().when(reservoirMock).ok()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder.clearServices().clearLastServicesCheckTimestamp()
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    getNextCheckState((res.nextState)).nextCheck.getMillis shouldBe (Now + CheckIntervalMillis) +- 10000
    verify(reservoirMock).ok()
    notification.get() shouldBe null
  }

  "don't notify about already expired appied services" in new Fixture {
    when(salesmanClient.getGoods(?, ?)(?)).thenReturn(Success(Nil))

    doNothing().when(reservoirMock).ok()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .clearServices()
      .clearLastServicesCheckTimestamp()
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.COLOR)
          .setCreated(Now - 1.day.toMillis)
          .setExpireDate(Now)
          .setIsActive(true)
      )
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    getNextCheckState((res.nextState)).nextCheck.getMillis shouldBe (Now + CheckIntervalMillis) +- 10000
    verify(reservoirMock).ok()
    notification.get() shouldBe null
  }

  "correctly check badges in private offers" in new Fixture {
    val builder = TestUtils.createOffer(dealer = false)
    when(salesmanUserClient.getProducts(?)(?)).thenReturn(
      Success(
        Seq(
          SalesmanUserClient.Product(
            builder.getOfferID,
            "ACTIVE",
            "badge",
            longToDateTime(Now - 1.hour.toMillis),
            longToDateTime(Now + 1.hour.toMillis)
          )
        )
      )
    )
    doNothing().when(reservoirMock).ok()

    builder.getOfferAutoruBuilder
      .clearServices()
      .clearLastServicesCheckTimestamp()
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.BADGE)
          .setCreated(Now)
          .setIsActive(true)
          .setBadge("foo")
      )
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.BADGE)
          .setCreated(Now)
          .setIsActive(true)
          .setBadge("bar")
      )
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    getNextCheckState((res.nextState)).nextCheck.getMillis shouldBe (Now + CheckIntervalMillis) +- 10000
    verify(reservoirMock).ok()
    notification.get() shouldBe null
  }

  "correctly notify about wrong badges in private offer" in new Fixture {
    pending
    val builder = TestUtils.createOffer(dealer = false)
    when(salesmanUserClient.getProducts(?)).thenReturn(
      Success(
        Seq(
          SalesmanUserClient.Product(
            builder.getOfferID,
            "ACTIVE",
            "special",
            longToDateTime(Now - 1.hour.toMillis),
            longToDateTime(Now + 1.hour.toMillis)
          )
        )
      )
    )
    doNothing().when(reservoirMock).error(ArgumentMatchers.any[String]())

    builder.getOfferAutoruBuilder
      .clearServices()
      .clearLastServicesCheckTimestamp()
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.BADGE)
          .setCreated(Now)
          .setIsActive(true)
          .setBadge("foo")
      )
      .addServices(
        PaidService
          .newBuilder()
          .setServiceType(ServiceType.BADGE)
          .setCreated(Now)
          .setIsActive(true)
          .setBadge("bar")
      )
    val offer = builder.build()
    val res = worker.process(offer, None)
    assert(worker.shouldProcess(offer, None).shouldProcess)
    res.nextState.get.toLong shouldBe (Now + CheckIntervalMillis) +- 10000

    notification.get().notAppliedProducts match {
      case Service(ServiceType.SPECIAL, None, _, _) :: Nil =>
    }
    notification.get().wrongAppliedProducts match {
      case Service(ServiceType.BADGE, Some(badge1), _, _) :: Service(ServiceType.BADGE, Some(badge2), _, _) :: Nil =>
        Set(badge1, badge2) shouldEqual Set("foo", "bar")
    }
  }
}
