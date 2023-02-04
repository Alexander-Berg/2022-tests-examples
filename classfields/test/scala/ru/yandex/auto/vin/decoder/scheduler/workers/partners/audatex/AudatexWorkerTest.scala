package ru.yandex.auto.vin.decoder.scheduler.workers.partners.audatex

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.{never, verify}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.common.{EventTypeSource, RawDataFetcher}
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.model.{AudatexDealers, VinCode}
import ru.yandex.auto.vin.decoder.partners.audatex.Audatex.AudatexPartner
import ru.yandex.auto.vin.decoder.partners.audatex.{Audatex, AudatexAudaHistory}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, StandardState, StateUpdateHistory}
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory.Status
import ru.yandex.auto.vin.decoder.raw.{RawToPreparedConverter, VinRawModel, VinRawModelWithEventSource}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.{ExecutionContext, Future}

class AudatexWorkerTest extends AnyWordSpecLike with MockitoSupport {
  implicit val m: MetricsSupport = TestOperationalSupport

  implicit protected val client = mock[RawDataFetcher[VinCode, VinRawModelWithEventSource]]
  implicit val tracer = NoopTracerFactory.create()
  implicit val t = Traced.empty
  protected val vinUpdateDao = mock[VinWatchingDao]
  protected val rateLimitMock = mock[RateLimiter]
  protected val queue = mock[WorkersQueue[VinCode, CompoundState]]
  protected val feature = mock[Feature[Boolean]]
  protected val enableRetriesPenalties = mock[Feature[Boolean]]
  private val audatexCache = mock[AudatexDealersCache]

  protected val rawStorageManager = mock[RawStorageManager[VinCode]]
  implicit protected val rawToPreparedConverter = mock[RawToPreparedConverter[VinRawModelWithEventSource]]
  protected val workerName = "worker-name"
  implicit val ec = ExecutionContext.global

  protected val vinCode = VinCode("WAUZZZF52HA005675")

  protected val rawModel = new VinRawModel with EventTypeSource {
    override val identifier: VinCode = vinCode
    override val raw: String = "[]"
    override val rawStatus: String = "200"
    override val groupId: String = ""

    override def source: EventType = EventType.UNDEFINED
  }

  protected val worker = AudatexWorker[VinRawModelWithEventSource](
    vinUpdateDao,
    queue,
    rawStorageManager,
    feature,
    workerName,
    AudatexAudaHistory(audatexCache),
    enableRetriesPenalties
  )
  when(enableRetriesPenalties.value).thenReturn(true)
  when(audatexCache.get).thenReturn(AudatexDealers(List.empty[AudatexPartner]))

  private val vin = VinCode("X7LBSRBYNBH480080")

  "AudatexWorker" should {

    val notExistingDealerClientId = 999999999
    val existingClientId = 27902L

    "disable processing if the dealer is disabled" in {
      val state = CompoundState
        .newBuilder()
        .setAudatex(
          StandardState
            .newBuilder()
            .addStateUpdateHistory(
              StateUpdateHistory
                .newBuilder()
                .setClientId(notExistingDealerClientId)
                .build()
            )
            .setShouldProcess(true)
        )
        .build()
      val stateHolder = WatchingStateHolder(vin, state, 0)

      val workResult = worker.action(stateHolder)
      assert(!workResult.reschedule)
      assert(workResult.updater.nonEmpty)

      val newState = workResult.updater.get.stateUpdate(state)
      assert(!newState.getAudatex.getShouldProcess)
      assert(newState.getAudatex.getStateUpdateHistoryList.isEmpty)

      verify(client, never()).fetch(?)(?, ?)
    }

    "handle failure without changing state otherwise" in {

      val state = CompoundState
        .newBuilder()
        .setAudatex(
          StandardState
            .newBuilder()
            .addStateUpdateHistory(
              StateUpdateHistory
                .newBuilder()
                .setClientId(existingClientId)
                .build()
            )
            .setShouldProcess(true)
        )
        .build()
      val stateHolder = WatchingStateHolder(vin, state, 0)

      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      when(audatexCache.get).thenReturn(
        AudatexDealers(List(AudatexPartner(Set(27902L), "", Audatex.Credentials("", ""))))
      )

      val workResult = worker.action(stateHolder)

      assert(!workResult.reschedule)
      assert(workResult.updater.nonEmpty)

      val newState = workResult.updater.get.stateUpdate(state)
      assert(newState.getAudatex.getShouldProcess)
      assert(!newState.getAudatex.getStateUpdateHistoryList.isEmpty)
    }

    "not filter trigger without client id" in {

      val state = CompoundState
        .newBuilder()
        .setAudatex(
          StandardState
            .newBuilder()
            .addStateUpdateHistory(
              StateUpdateHistory
                .newBuilder()
                .build()
            )
            .setShouldProcess(true)
        )
        .build()
      val stateHolder = WatchingStateHolder(vin, state, 0)

      when(client.fetch(?)(?, ?)).thenReturn(Future.failed(new RuntimeException))

      val workResult = worker.action(stateHolder)

      assert(!workResult.reschedule)
      assert(workResult.updater.nonEmpty)

      val newState = workResult.updater.get.stateUpdate(state)
      assert(newState.getAudatex.getShouldProcess)
      assert(!newState.getAudatex.getStateUpdateHistoryList.isEmpty)
    }

    "remove disabled trigger on multiple triggers" in {

      val state = CompoundState
        .newBuilder()
        .setAudatex(
          StandardState
            .newBuilder()
            .addStateUpdateHistory(
              StateUpdateHistory
                .newBuilder()
                .setClientId(existingClientId)
                .build()
            )
            .addStateUpdateHistory(
              StateUpdateHistory
                .newBuilder()
                .setClientId(notExistingDealerClientId)
                .build()
            )
            .setShouldProcess(true)
        )
        .build()
      val stateHolder = WatchingStateHolder(vin, state, 0)

      when(client.fetch(?)(?, ?)).thenReturn(Future.successful(rawModel))

      val history = VinInfoHistory
        .newBuilder()
        .setEventType(rawModel.source)
        .setVin(rawModel.identifier.toString)
        .setStatus(Status.OK)
        .build()

      when(rawToPreparedConverter.convert(?)(?)).thenReturn(Future.successful(history))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.successful(()))

      val workResult = worker.action(stateHolder)

      assert(!workResult.reschedule)
      assert(workResult.updater.nonEmpty)

      val newState = workResult.updater.get.stateUpdate(state)
      assert(!newState.getAudatex.getShouldProcess)
      assert(newState.getAudatex.getStateUpdateHistoryList.isEmpty)
    }
  }

}
