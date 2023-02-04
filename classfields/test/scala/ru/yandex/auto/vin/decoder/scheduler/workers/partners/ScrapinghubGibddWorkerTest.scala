package ru.yandex.auto.vin.decoder.scheduler.workers.partners

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.scrapinghub.gibdd.{ScrapingHubGibddClient, ScrapingHubGibddReportType}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, ScrapinghubGibddState, StateUpdateHistory}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.RawToPreparedConverter
import ru.yandex.auto.vin.decoder.raw.scrapinghub.RegistrationRawModel
import ru.yandex.auto.vin.decoder.scheduler.models.{WatchingStateHolder, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.workers.WorkResult
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.storage.ShardedMySql
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class ScrapinghubGibddWorkerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  implicit val m: OperationalSupport = TestOperationalSupport
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val tracer = NoopTracerFactory.create()
  implicit val t = Traced.empty
  implicit val trigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val vin = VinCode("ХWЕНС812АА0001038")

  private def getRaw(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/scrapinghub/$filename")
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }

  val raw = getRaw("registration/registration-success.json")

  private val registrationRawModel = RegistrationRawModel(
    "200",
    raw,
    vin,
    None
  )

  private val registrationPrepared = VinInfoHistory
    .newBuilder()
    .setEventType(EventType.SH_GIBDD_REGISTRATION)
    .setVin(vin.toString)
    .build()

  private class Fixture {
    val reportType = ScrapingHubGibddReportType.Registration
    val gibddClient = mock[ScrapingHubGibddClient]
    val converter = mock[RawToPreparedConverter[RegistrationRawModel]]
    val queue = mock[WorkersQueue[VinCode, CompoundState]]
    val rawStorageManager = mock[RawStorageManager[VinCode]]
    val rateLimiter = mock[RateLimiter]
    val feature = mock[Feature[Boolean]]
    private val vinUpdateDao = mock[VinWatchingDao]
    private val mysql = mock[ShardedMySql]

    val worker: ScrapinghubGibddWorker[RegistrationRawModel] = new ScrapinghubGibddWorker(
      reportType,
      gibddClient,
      converter,
      queue,
      rawStorageManager,
      rateLimiter,
      feature,
      vinUpdateDao
    )
  }

  "ScrapinghubGibddWorker.action" should {
    "throw exception if no gibdd state" in new Fixture {
      val b = CompoundState.newBuilder.build()
      intercept[IllegalArgumentException] {
        worker.action(WatchingStateHolder(vin, b, 1))
      }
    }
    "skip if no gibdd state with should process" in new Fixture {
      val b = CompoundState.newBuilder
      b.getScrapinghubGibddStateBuilder
        .getReportBuilder(reportType)
        .setShouldProcess(false)

      val res = worker.action(WatchingStateHolder(vin, b.build(), 1))
      assert(res.updater.isEmpty)
      assert(!res.reschedule)
    }
    "reschedule with delay no more 10 minutes if try rate limit failed" in new Fixture {
      val b = CompoundState.newBuilder
      b.getScrapinghubGibddStateBuilder
        .getReportBuilder(reportType)
        .setProcessRequested(System.currentTimeMillis())
        .setShouldProcess(true)

      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(false)
      val res: WorkResult[CompoundState] = worker.action(WatchingStateHolder(vin, b.build(), 1))
      assert(res.updater.nonEmpty)
      assert(res.updater.get.delay().toDuration.toMillis <= 10.minutes.toMillis)
    }
    "success get report" in new Fixture {
      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(gibddClient.getGibddReportByType[RegistrationRawModel](?, ?)(?, ?))
        .thenReturn(Future.successful(registrationRawModel))
      when(converter.convert(?)(?)).thenReturn(Future.successful(registrationPrepared))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.successful(()))

      val b = CompoundState.newBuilder
      b.getScrapinghubGibddStateBuilder
        .getReportBuilder(reportType)
        .setShouldProcess(true)
        .setProcessRequested(System.currentTimeMillis())
        .addStateUpdateHistory(StateUpdateHistory.newBuilder().setTimestamp(123L))

      val state: WatchingStateHolder[VinCode, CompoundState] = WatchingStateHolder(vin, b.build(), 1)
      val res: WorkResult[CompoundState] = worker.action(state)
      val updated: WatchingStateUpdate[CompoundState] = res.updater.get(state.toUpdate)
      val reportOpt: Option[ScrapinghubGibddState.GibddReport] =
        updated.state.getScrapinghubGibddState.findReport(reportType.toString)

      assert(res.updater.nonEmpty)
      assert(!res.reschedule)
      assert(reportOpt.nonEmpty)
      assert(!reportOpt.get.getShouldProcess)
      assert(reportOpt.get.getLastCheck > 0)
      assert(reportOpt.get.getStateUpdateHistoryCount === 0)

      verify(gibddClient, times(1)).getGibddReportByType(?, ?)(?, ?)
      verify(converter, times(1)).convert(?)(?)
      verify(rawStorageManager, times(1)).upsert(?)(?)
    }

    "reschedule if get report request failed" in new Fixture {
      when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)
      when(gibddClient.getGibddReportByType(?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))

      val b: CompoundState.Builder = CompoundState.newBuilder
      b.getScrapinghubGibddStateBuilder
        .getReportBuilder(reportType)
        .setProcessRequested(System.currentTimeMillis())
        .setShouldProcess(true)

      val state: WatchingStateHolder[VinCode, CompoundState] = WatchingStateHolder(vin, b.build(), 1)
      val res: WorkResult[CompoundState] = worker.action(state)
      val updated: WatchingStateUpdate[CompoundState] = res.updater.get(state.toUpdate)
      val reportOpt: Option[ScrapinghubGibddState.GibddReport] =
        updated.state.getScrapinghubGibddState.findReport(reportType.toString)

      assert(res.updater.nonEmpty)
      assert(res.updater.get.delay().toDuration.toMillis <= 15.minutes.toMillis)
      assert(!res.reschedule)

      assert(reportOpt.nonEmpty)
      assert(reportOpt.get.getShouldProcess)
      assert(reportOpt.get.getLastCheck == 0)

      verify(gibddClient, times(1)).getGibddReportByType(vin, reportType)
      verify(converter, never()).convert(?)(?)
      verify(rawStorageManager, never()).upsert(?)(?)
    }
  }

  "reschedule if converter failed" in new Fixture {
    when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

    when(gibddClient.getGibddReportByType[RegistrationRawModel](?, ?)(?, ?))
      .thenReturn(Future.successful(registrationRawModel))
    when(converter.convert(?)(?)).thenReturn(Future.failed(new RuntimeException("")))

    val b: CompoundState.Builder = CompoundState.newBuilder

    b.getScrapinghubGibddStateBuilder
      .getReportBuilder(reportType)
      .setProcessRequested(System.currentTimeMillis())
      .setShouldProcess(true)

    val state: WatchingStateHolder[VinCode, CompoundState] = WatchingStateHolder(vin, b.build(), 1)
    val res: WorkResult[CompoundState] = worker.action(state)
    val updated: WatchingStateUpdate[CompoundState] = res.updater.get(state.toUpdate)

    val reportOpt: Option[ScrapinghubGibddState.GibddReport] =
      updated.state.getScrapinghubGibddState.findReport(reportType.toString)

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().toDuration.toMillis <= 15.minutes.toMillis)
    assert(!res.reschedule)

    assert(reportOpt.nonEmpty)
    assert(reportOpt.get.getShouldProcess)
    assert(reportOpt.get.getLastCheck == 0)

    verify(gibddClient, times(1)).getGibddReportByType(vin, reportType)
    verify(converter, times(1)).convert(?)(?)
    verify(rawStorageManager, never()).upsert(?)(?)
  }

  "reschedule if append to raw storage failed" in new Fixture {
    when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

    when(gibddClient.getGibddReportByType[RegistrationRawModel](?, ?)(?, ?))
      .thenReturn(Future.successful(registrationRawModel))
    when(converter.convert(?)(?)).thenReturn(Future.successful(registrationPrepared))
    when(rawStorageManager.upsert(?)(?)).thenReturn(Future.failed(new RuntimeException("")))

    val b: CompoundState.Builder = CompoundState.newBuilder

    b.getScrapinghubGibddStateBuilder
      .getReportBuilder(reportType)
      .setProcessRequested(System.currentTimeMillis())
      .setShouldProcess(true)

    val state: WatchingStateHolder[VinCode, CompoundState] = WatchingStateHolder(vin, b.build(), 1)
    val res: WorkResult[CompoundState] = worker.action(state)
    val updated: WatchingStateUpdate[CompoundState] = res.updater.get(state.toUpdate)

    val reportOpt: Option[ScrapinghubGibddState.GibddReport] =
      updated.state.getScrapinghubGibddState.findReport(reportType.toString)

    assert(res.updater.nonEmpty)
    assert(res.updater.get.delay().toDuration.toMillis <= 15.minutes.toMillis)
    assert(!res.reschedule)

    assert(reportOpt.nonEmpty)
    assert(reportOpt.get.getShouldProcess)
    assert(reportOpt.get.getLastCheck == 0)

    verify(gibddClient, times(1)).getGibddReportByType(vin, reportType)
    verify(converter, times(1)).convert(?)(?)
    verify(rawStorageManager, times(1)).upsert(?)(?)
  }

}
