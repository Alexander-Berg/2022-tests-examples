package auto.dealers.multiposting.scheduler.test

import java.time.{OffsetDateTime, ZoneOffset, ZonedDateTime}
import common.zio.clients.kv.KvClient.NoKeyError
import common.zio.clock.MoscowClock
import io.prometheus.client.CollectorRegistry
import ru.auto.api.api_offer_model.{Category, OfferStatus, Section}
import auto.dealers.multiposting.model._
import auto.dealers.multiposting.scheduler.task.WarehouseStateFromYtToPostgresTask
import auto.dealers.multiposting.storage.testkit.{EodProcessingDateDaoMock, FullOfferEodDaoMock, WarehouseStateDaoMock}
import common.ops.prometheus.CollectorRegistryWrapper
import common.zio.app.AppInfo
import auto.dealers.multiposting.storage.testkit.WarehouseStateDaoMock
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.mock.Expectation._
import zio.test._
import zio.test.mock.MockClock

object WarehouseStateFromYtToPostgresTaskSpec extends DefaultRunnableSpec {

  val task =
    new WarehouseStateFromYtToPostgresTask(new CollectorRegistryWrapper(CollectorRegistry.defaultRegistry), AppInfo())

  val nowOdt: OffsetDateTime = OffsetDateTime.of(2021, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
  val withTz: ZonedDateTime = nowOdt.atZoneSameInstant(MoscowClock.timeZone)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("WarehouseStateFromYtToPostgresTask")(
      firstRun,
      shouldSkipRun,
      shouldRun
    ) @@ sequential

  private val firstRun = testM("First run (no saved timestamp)") {
    val state = OfferEodState(
      dealerId = "1",
      cardId = "1",
      vin = "vin",
      changeVersion = "1",
      category = "CARS",
      section = "NEW",
      autoruStatus = Some("ACTIVE"),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val expected = WarehouseState(
      day = withTz.toLocalDate,
      clientId = ClientId(1),
      cardId = CardId(1),
      vin = Vin("vin"),
      changeVersion = 1,
      category = Category.CARS,
      section = Section.NEW,
      autoruStatus = Some(OfferStatus.ACTIVE),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val zkMock = EodProcessingDateDaoMock.Get(failure(NoKeyError("no key", None))) ++ EodProcessingDateDaoMock.Set(
      equalTo(withTz.plusDays(1).toInstant),
      unit
    )
    val eodMock = FullOfferEodDaoMock.FetchOfferEodState(equalTo(withTz), value(Seq(state)))
    val stateMock =
      WarehouseStateDaoMock
        .UpsertBatch(
          equalTo(Seq(expected, expected.copy(day = withTz.toLocalDate.plusDays(1), changeVersion = 0))),
          unit
        )
    val clockMock = MockClock.CurrentDateTime(value(nowOdt))

    assertM(task.program)(isUnit)
      .provideCustomLayer(Blocking.live ++ clockMock ++ zkMock ++ eodMock ++ stateMock)
  }

  private val shouldSkipRun = testM("Should skip run") {
    val state = OfferEodState(
      dealerId = "1",
      cardId = "1",
      vin = "vin",
      changeVersion = "1",
      category = "CARS",
      section = "NEW",
      autoruStatus = Some("ACTIVE"),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val expected = WarehouseState(
      day = withTz.toLocalDate,
      clientId = ClientId(1),
      cardId = CardId(1),
      vin = Vin("vin"),
      changeVersion = 1,
      category = Category.CARS,
      section = Section.NEW,
      autoruStatus = Some(OfferStatus.ACTIVE),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val zkMock = EodProcessingDateDaoMock.Get(value(withTz.minusHours(1).toInstant))
    val eodMock = FullOfferEodDaoMock.FetchOfferEodState(equalTo(withTz), value(Seq(state))).atMost(0)
    val stateMock =
      WarehouseStateDaoMock
        .UpsertBatch(
          equalTo(Seq(expected, expected.copy(day = withTz.toLocalDate.plusDays(1), changeVersion = 0))),
          unit
        )
        .atMost(0)
    val clockMock = MockClock.CurrentDateTime(value(nowOdt))

    assertM(task.program)(isUnit)
      .provideCustomLayer(clockMock ++ zkMock ++ eodMock ++ stateMock)
  }

  private val shouldRun = testM("Should run") {
    val state = OfferEodState(
      dealerId = "1",
      cardId = "1",
      vin = "vin",
      changeVersion = "1",
      category = "CARS",
      section = "NEW",
      autoruStatus = Some("ACTIVE"),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val expected = WarehouseState(
      day = withTz.toLocalDate,
      clientId = ClientId(1),
      cardId = CardId(1),
      vin = Vin("vin"),
      changeVersion = 1,
      category = Category.CARS,
      section = Section.NEW,
      autoruStatus = Some(OfferStatus.ACTIVE),
      avitoStatus = None,
      dromStatus = None,
      globalStatus = None
    )

    val zkMock = EodProcessingDateDaoMock.Get(value(withTz.toInstant)) ++ EodProcessingDateDaoMock.Set(
      equalTo(withTz.plusDays(1).toInstant),
      unit
    )
    val eodMock = FullOfferEodDaoMock.FetchOfferEodState(equalTo(withTz), value(Seq(state)))
    val stateMock =
      WarehouseStateDaoMock
        .UpsertBatch(
          equalTo(Seq(expected, expected.copy(day = withTz.toLocalDate.plusDays(1), changeVersion = 0))),
          unit
        )

    // plus one day
    val clockMock = MockClock.CurrentDateTime(value(nowOdt.plusDays(1)))

    assertM(task.program)(isUnit)
      .provideCustomLayer(Blocking.live ++ clockMock ++ zkMock ++ eodMock ++ stateMock)
  }
}
