package ru.yandex.auto.vin.decoder.scheduler.workers.partners.unified

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, Ignore}
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.common.{EventTypeSource, RawDataFetcher}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, RetriesState, StateUpdateHistory}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.raw.{RawToPreparedConverter, VinRawModel, VinRawModelWithEventSource}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.state.StandardPartnerState
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

@Ignore
class StandardPartnerWorkerTest(partner: StandardPartnerState)
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter {

  implicit val m: MetricsSupport = TestOperationalSupport

  implicit protected val client = mock[RawDataFetcher[VinCode, VinRawModelWithEventSource]]
  implicit val tracer = NoopTracerFactory.create()
  implicit val t = Traced.empty
  protected val vinUpdateDao = mock[VinWatchingDao]
  protected val rateLimitMock = mock[RateLimiter]
  protected val queue = mock[WorkersQueue[VinCode, CompoundState]]
  protected val feature = mock[Feature[Boolean]]
  protected val retryDelaysFeature = mock[Feature[Boolean]]
  protected val rawStorageManager = mock[RawStorageManager[VinCode]]
  implicit protected val rawToPreparedConverter = mock[RawToPreparedConverter[VinRawModelWithEventSource]]
  protected val workerName = "worker-name"

  protected val vinCode = VinCode("WAUZZZF52HA005675")

  protected val rawModel = new VinRawModel with EventTypeSource {
    override val identifier: VinCode = vinCode
    override val raw: String = "[]"
    override val rawStatus: String = "200"
    override val groupId: String = ""

    override def source: EventType = EventType.UNDEFINED
  }

  protected val worker = StandardPartnerWorker[VinCode, VinRawModelWithEventSource](
    vinUpdateDao,
    queue,
    rawStorageManager,
    feature,
    workerName,
    partner,
    retryDelaysFeature
  )

  private val compoundState: CompoundState = {
    val state = partner
      .getState(CompoundState.newBuilder.build)
      .toBuilder
      .setShouldProcess(true)
      .addStateUpdateHistory(StateUpdateHistory.getDefaultInstance)
      .build
    partner.setState(CompoundState.newBuilder.build, state)
  }

  before {
    reset(client)
    reset(rawStorageManager)
  }

  "UnifiedWorker" should {

    "process correct response" in {

      when(retryDelaysFeature.value).thenReturn(true)
      when(client.fetch(?)(?, ?)).thenReturn(Future.successful(rawModel))

      val history = VinInfoHistory
        .newBuilder()
        .setEventType(rawModel.source)
        .setVin(rawModel.identifier.toString)
        .setStatus(Status.OK)
        .build()

      when(rawToPreparedConverter.convert(?)(?)).thenReturn(Future.successful(history))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.successful(()))

      val holder = WatchingStateHolder(vinCode, compoundState, System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      verify(rawStorageManager, times(1)).upsert(?)(?)

      assert(update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getLastCheck != 0)
      assert(partner.getState(update.state).getRetriesState.getCounter == 0)
      assert(!partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getStateUpdateHistoryCount == 0)
    }

    "process exception correctly while converting model" in {

      when(client.fetch(?)(?, ?)).thenReturn(Future.successful(rawModel))

      when(rawToPreparedConverter.convert(?)(?)).thenReturn(Future.failed(new RuntimeException("")))

      val holder = WatchingStateHolder(vinCode, compoundState, System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getRetriesState.getCounter == 1)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getStateUpdateHistoryCount != 0)

    }

    "process exception correctly in case when db is unavailable" in {

      when(retryDelaysFeature.value).thenReturn(true)
      when(client.fetch(?)(?, ?)).thenReturn(Future.successful(rawModel))

      val history = VinInfoHistory
        .newBuilder()
        .setEventType(rawModel.source)
        .setVin(rawModel.identifier.toString)
        .setStatus(Status.OK)
        .build()

      when(rawToPreparedConverter.convert(?)(?)).thenReturn(Future.successful(history))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.failed(new RuntimeException("")))
      val holder = WatchingStateHolder(vinCode, compoundState, System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)
      verify(rawStorageManager, times(1)).upsert(?)(?)
      assert(partner.getState(update.state).getRetriesState.getCounter == 1)
      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getStateUpdateHistoryCount != 0)
    }

    "process exception correctly in case when client responds with error" in {

      when(retryDelaysFeature.value).thenReturn(true)
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))

      val holder = WatchingStateHolder(vinCode, compoundState, System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 1)
      assert(partner.getState(update.state).getStateUpdateHistoryCount != 0)
    }

    "reschedule job in case cannot acquire rate limit" in {

      when(retryDelaysFeature.value).thenReturn(true)
      val limiter = mock[RateLimiter]
      val workerInstance = StandardPartnerWorker[VinCode, VinRawModelWithEventSource](
        vinUpdateDao,
        queue,
        rawStorageManager,
        feature,
        workerName,
        partner,
        retryDelaysFeature
      )

      when(limiter.tryAcquire(?, ?, ?)).thenReturn(false)
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))

      val holder = WatchingStateHolder(vinCode, compoundState, System.currentTimeMillis())
      val res = workerInstance.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 1)
      assert(partner.getState(update.state).getStateUpdateHistoryCount != 0)

    }

    "reschedule processing with no work done if there is a retry penalty" in {
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))
      val failingState = partner
        .getState(compoundState)
        .toBuilder
        .setRetriesState(
          RetriesState
            .newBuilder()
            .setCounter(15)
            .setLastRetry(System.currentTimeMillis())
        )
        .build()
      val holder =
        WatchingStateHolder(vinCode, partner.setState(compoundState, failingState), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 15)
      val updateDuration = res.updater.get.delay().toDuration
      assert(updateDuration == 15.minutes)
      assert(partner.getState(update.state).getStateUpdateHistoryCount != 0)
      verify(client, times(0)).fetch(?)(?, ?)
    }

    "add a retry penalty if a request has failed for multiple times" in {
      when(retryDelaysFeature.value).thenReturn(true)
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))
      val failingState = partner
        .getState(compoundState)
        .toBuilder
        .setRetriesState(
          RetriesState
            .newBuilder()
            .setCounter(6)
            .setLastRetry(System.currentTimeMillis() - 10.minutes.toMillis)
        )
        .build()
      val holder =
        WatchingStateHolder(vinCode, partner.setState(compoundState, failingState), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 7)
      val updateDuration = res.updater.get.delay().toDuration
      assert(updateDuration == 6.minutes)
    }

    "add no retry penalty if a request has failed 1-n allowed times" in {
      when(retryDelaysFeature.value).thenReturn(true)
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))
      val failingState = partner
        .getState(compoundState)
        .toBuilder
        .setRetriesState(
          RetriesState
            .newBuilder()
            .setCounter(3)
            .setLastRetry(System.currentTimeMillis())
        )
        .build()
      val holder =
        WatchingStateHolder(vinCode, partner.setState(compoundState, failingState), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 4)
      val updateDuration = res.updater.get.delay().toDuration
      assert(updateDuration <= 5.minutes)
    }

    "keep default processing logic on disabled feature" in {
      when(retryDelaysFeature.value).thenReturn(false)
      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException("")))
      val failingState = partner
        .getState(compoundState)
        .toBuilder
        .setRetriesState(
          RetriesState
            .newBuilder()
            .setCounter(15)
            .setLastRetry(System.currentTimeMillis())
        )
        .build()
      val holder =
        WatchingStateHolder(vinCode, partner.setState(compoundState, failingState), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(partner.getState(update.state).getShouldProcess)
      assert(partner.getState(update.state).getRetriesState.getCounter == 15)
      val updateDuration = res.updater.get.delay().toDuration
      assert(updateDuration >= 1.minutes && updateDuration <= 15.minutes)
    }

  }
}
