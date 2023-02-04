package ru.yandex.auto.vin.decoder.scheduler.workers.partners

import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.manager.vin.adaperio.VinAdaperioManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioExceptions.{
  AdaperioInvalidVinError,
  AdaperioOrderNotFound,
  AdaperioReportNotReadyYet
}
import ru.yandex.auto.vin.decoder.partners.adaperio.model.report.AdaperioReportOldMain
import ru.yandex.auto.vin.decoder.partners.adaperio.{AdaperioClient, AdaperioReportType}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.raw.adaperio.AdaperioRawModel
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.MixedRateLimiter
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.vos.VosNotifier
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Future
import scala.concurrent.duration._

class AdaperioWorkerTest extends AnyFunSuite with MockitoSupport with BeforeAndAfterAll {
  private val adaperioClient = mock[AdaperioClient]
  private val adaperioManager = mock[VinAdaperioManager]
  private val vosNotifier = mock[VosNotifier]
  private val rateLimiter = MixedRateLimiter(100, 100, 1)
  private val vinUpdateDao = mock[VinWatchingDao]
  private val queue = mock[WorkersQueue[VinCode, CompoundState]]
  private val feature = mock[Feature[Boolean]]

  private val vin = VinCode("X4X3D59430PS96744")

  private val reportType = AdaperioReportType.Main

  implicit val t: Traced = Traced.empty
  implicit private val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()

  override def beforeAll(): Unit = {
    reset(adaperioClient)
    reset(vosNotifier)
  }

  val worker = new AdaperioWorker(
    reportType,
    adaperioClient,
    adaperioManager,
    vosNotifier,
    vinUpdateDao,
    queue,
    feature,
    rateLimiter
  )

  test("ignore if no adaperio") {
    val b = CompoundState.newBuilder().build()

    intercept[IllegalArgumentException] {
      worker.action(WatchingStateHolder(vin, b, 1))
    }
  }

  test("ignore if completed and no flags") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setRequestSent(5)
      .setReportArrived(55)

    intercept[IllegalArgumentException] {
      worker.action(WatchingStateHolder(vin, b.build(), 1))
    }
  }

  test("finish if invalid") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder.setInvalid(true).setShouldProcess(true)
    val state = WatchingStateHolder(vin, b.build(), 1)

    val res = worker.action(state)

    assert(res.updater.isDefined)
    assert(!res.reschedule)

    val updated = res.updater.get(state.toUpdate)
    val report = updated.state.getAdaperio.findReport(reportType.toString).get

    assert(!report.getShouldProcess)
    assert(!report.getForceUpdate)
    assert(report.getStateUpdateHistoryCount == 0)
  }

  test("send request if should process") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setShouldProcess(true)

    val state = WatchingStateHolder(vin, b.build(), 1)

    when(adaperioClient.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(545L))
    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException()))

    val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)

    val updated = res.updater.get(state.toUpdate)
    val reportOpt = updated.state.getAdaperio.findReport(reportType.toString)

    assert(reportOpt.exists(_.getRequestSent != 0))
    assert(reportOpt.exists(_.getReportId == 545))
  }

  test("process invalid") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setShouldProcess(true)

    when(adaperioClient.postOrder(?, ?)(?, ?)).thenReturn(Future.failed(AdaperioInvalidVinError("invalid vin")))

    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().isDefault)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    assert(adaperio.getInvalid)
  }

  test("process order not found") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setRequestSent(1)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(AdaperioOrderNotFound("no order")))

    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().isDefault)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    assert(adaperio.findReport(reportType).exists(_.getNoOrder))
  }

  test("regenerate report if completed but force update") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setForceUpdate(true)
      .setRequestSent(5)
      .setReportArrived(55)
      .setReportId(34252L)

    val state = WatchingStateHolder(vin, b.build(), 1)

    when(adaperioClient.postOrder(?, ?)(?, ?)).thenReturn(Future.successful(11L))
    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException()))
    val res = worker.action(state)

    assert(res.updater.nonEmpty)
    assert(!res.updater.get.delay().isDefault)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    assert(reportOpt.nonEmpty)

    for (report <- reportOpt) {
      assert(report.getRequestSent != 5)
      assert(report.getReportArrived == 55)
      assert(report.getReportId == 11L)
    }
  }

  test("reschedule if report not ready") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setForceUpdate(true)
      .setRequestSent(55)
      .setReportArrived(1)
      .setReportId(34252L)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(AdaperioReportNotReadyYet(Set.empty)))
    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().toDuration == 1.minute)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    assert(reportOpt.nonEmpty)

    for (report <- reportOpt) {
      assert(report.getRequestSent == 55L)
      assert(report.getReportArrived == 1L)
      assert(report.getReportId == 34252L)
    }
  }

  test("reschedule if report not ready using counter for delay") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setForceUpdate(true)
      .setRequestSent(55)
      .setReportArrived(1)
      .setReportId(34252L)
      .setCounter(10)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(AdaperioReportNotReadyYet(Set.empty)))
    val state = WatchingStateHolder(vin, b.build(), 1)

    val res = worker.action(state)

    assert(res.updater.get.delay().toDuration == 4.minutes)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    assert(reportOpt.nonEmpty)

    for (report <- reportOpt) {
      assert(report.getRequestSent == 55L)
      assert(report.getCounter == 11)
      assert(report.getReportArrived == 1L)
      assert(report.getReportId == 34252L)
    }
  }

  test("reschedule if report not ready using counter for delay but no more then 3 hours") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setForceUpdate(true)
      .setRequestSent(55)
      .setReportArrived(1)
      .setReportId(34252L)
      .setCounter(100)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(AdaperioReportNotReadyYet(Set.empty)))
    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(res.updater.get.delay().toDuration == 3.hour)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    assert(reportOpt.nonEmpty)

    for (report <- reportOpt) {
      assert(report.getRequestSent == 55L)
      assert(report.getReportArrived == 1L)
      assert(report.getReportId == 34252L)
    }
  }

  test("reschedule if any error getting report") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setRequestSent(55)
      .setReportArrived(1)
      .setReportId(34252L)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException()))
    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(!res.updater.get.delay().isDefault)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    for (report <- reportOpt) {
      assert(report.getRequestSent == 55L)
      assert(report.getReportArrived == 1L)
      assert(report.getReportId == 34252L)
    }
  }

  test("save if all ok") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setForceUpdate(true)
      .setRequestSent(55)
      .setReportArrived(1)
      .setReportId(34252L)

    when(adaperioClient.getReportByVin(?, ?, ?)(?, ?)).thenReturn(
      Future.successful(AdaperioRawModel("", "200", VinCode.apply("WP0ZZZ97ZCL081102"), AdaperioWorkerTest.report))
    )
    when(vosNotifier.asyncNotify(?)(?)).thenReturn(Future.successful(()))
    when(adaperioManager.update(?, ?, ?)(?)).thenReturn(Future.unit)

    val state = WatchingStateHolder(vin, b.build(), 1)
    val res = worker.action(state)

    assert(res.updater.get.delay().isDefault)

    val adaperio = res.updater.get(state.toUpdate).state.getAdaperio
    val reportOpt = adaperio.findReport(reportType.toString)

    for (report <- reportOpt) {
      assert(report.getRequestSent == 55L)
      assert(report.getReportArrived != 1L)
      assert(report.getReportId == 34252L)
      assert(!report.getForceUpdate)
    }
  }
}

object AdaperioWorkerTest {

  def report: AdaperioReportOldMain = {
    val json = Json.parse(reportJson)
    json.as[AdaperioReportOldMain]
  }

  val reportJson: String =
    """
      |{
      |  "Dtp": {
      |    "Dtps": [
      |      {
      |        "Vin": "WP0ZZZ97ZCL081102",
      |        "Region": "Краснодарский край",
      |        "Status": false,
      |        "CarMark": "PORSCHE",
      |        "CarYear": 2011,
      |        "CarModel": "Cayenne",
      |        "CrashDate": "2016-03-12T21:30:00",
      |        "DamageState": "Повреждено",
      |        "AccidentType": "Столкновение",
      |        "DamagePoints": ["04"],
      |        "AccidentNumber": "30020133"
      |      }
      |    ],
      |    "Status": "OK",
      |    "StatusCode": 0
      |  },
      |  "OrderId": 828659692,
      |  "BankPledged": {
      |    "Vin": "WP0ZZZ97ZCL081102",
      |    "Status": "OK",
      |    "History": [],
      |    "OrderId": 828659692,
      |    "StatusCode": 0,
      |    "HasRestrictions": false
      |  },
      |  "DrivingAway": {
      |    "Status": "OK",
      |    "StatusCode": 0,
      |    "DrivingAways": []
      |  },
      |  "BaseInfoStatus": {
      |    "Status": "OK",
      |    "BaseInfo": {
      |      "Vin": "WP0ZZZ97ZCL081102",
      |      "Name": "ПОРШЕ ПАНАМЕРА ТУРБО",
      |      "Year": 2011,
      |      "Color": "СЕРЫЙ",
      |      "Power": 500.5,
      |      "Volume": 4806.0,
      |      "Category": "В",
      |      "MaxWeight": 2500,
      |      "WheelForm": "4X2",
      |      "BodyNumber": "C02318",
      |      "EngineNumber": "С02318"
      |    },
      |    "StatusCode": 0
      |  },
      |  "MvdRestriction": {
      |    "Status": "OK",
      |    "StatusCode": 0,
      |    "MvdRestrictions": []
      |  },
      |  "OwnershipPeriod": {
      |    "Status": "OK",
      |    "StatusCode": 0,
      |    "OwnershipPeriods": [
      |      {
      |        "To": "2014-02-07T00:00:00",
      |        "From": "2013-01-29T00:00:00",
      |        "Owner": "LEGAL",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "03"
      |      },
      |      {
      |        "To": "2015-12-15T00:00:00",
      |        "From": "2014-02-07T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "03"
      |      },
      |      {
      |        "To": "2016-09-01T00:00:00",
      |        "From": "2015-12-15T00:00:00",
      |        "Owner": "LEGAL",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "11"
      |      },
      |      {
      |        "To": "2016-11-06T00:00:00",
      |        "From": "2016-09-01T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "09"
      |      },
      |      {
      |        "To": "2017-05-31T00:00:00",
      |        "From": "2016-11-06T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "03"
      |      },
      |      {
      |        "To": "2017-09-01T00:00:00",
      |        "From": "2017-06-09T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "03"
      |      },
      |      {
      |        "To": "2017-09-04T00:00:00",
      |        "From": "2017-09-01T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "05"
      |      },
      |      {
      |        "To": "2017-09-19T00:00:00",
      |        "From": "2017-09-04T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "04"
      |      },
      |      {
      |        "To": "2018-12-21T00:00:00",
      |        "From": "2017-09-19T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "11"
      |      },
      |      {
      |        "From": "2018-12-21T00:00:00",
      |        "Owner": "PERSON",
      |        "Operation": "Смена владельца",
      |        "OperationCode" : "03"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin
}
