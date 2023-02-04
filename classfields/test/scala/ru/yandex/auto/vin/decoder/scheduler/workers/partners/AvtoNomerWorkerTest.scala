package ru.yandex.auto.vin.decoder.scheduler.workers.partners

import auto.carfax.common.clients.avatars.AvatarsClient
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.avtonomer.{AvtonomerClient, AvtonomerRawModel}
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.searcher.unification.Unificator
import ru.yandex.auto.vin.decoder.storage.licenseplate.LicensePlateWatchingDao
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AvtoNomerWorkerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  implicit val m: MetricsSupport = TestOperationalSupport
  implicit val t: Traced = Traced.empty
  implicit val tracer = NoopTracerFactory.create()

  private val avtonomerClient = mock[AvtonomerClient]
  private val avatarnica = mock[AvatarsClient]
  private val unificator = mock[Unificator]
  private val rateLimiter = RateLimiter.create(1000)
  private val lpUpdateDao = mock[LicensePlateWatchingDao]
  private val queue = mock[WorkersQueue[LicensePlate, CompoundState]]
  private val feature = mock[Feature[Boolean]]
  private val rawStorageManager = mock[RawStorageManager[LicensePlate]]

  private val worker: AvtoNomerWorker = new AvtoNomerWorker(
    avtonomerClient,
    avatarnica,
    unificator,
    rawStorageManager,
    rateLimiter,
    feature,
    lpUpdateDao,
    queue
  )

  private val lp = LicensePlate("а123аа77")

  private val responseWithRecords = AvtonomerRawModel.apply(
    "{\n  \"error\": 0,\n  \"region\": \"98\",\n  \"informer\": \"http://img03.platesmania.com/190601/inf/129491219bb2e4.png\",\n  \"cars\": [\n    {\n      \"make\": \"Mercedes-Benz\",\n      \"model\": \"S-Klasse\",\n      \"date\": \"2012-10-13 17:45:35\",\n      \"photo\": {\n        \"link\": \"http://avto-nomer.ru/ru/nomer3016137\",\n        \"small\": \"http://img02.avto-nomer.ru/011/s/ru3016137.jpg\",\n        \"medium\": \"http://img02.avto-nomer.ru/011/m/ru3016137.jpg\",\n        \"original\": \"http://img02.avto-nomer.ru/011/o/ru3016137.jpg\"\n      }\n    },\n    {\n      \"make\": \"Mercedes-Benz\",\n      \"model\": \"S-Klasse\",\n      \"date\": \"2012-10-14 21:25:10\",\n      \"photo\": {\n        \"link\": \"http://avto-nomer.ru/ru/nomer3019690\",\n        \"small\": \"http://img02.avto-nomer.ru/011/s/ru3019690.jpg\",\n        \"medium\": \"http://img02.avto-nomer.ru/011/m/ru3019690.jpg\",\n        \"original\": \"http://img02.avto-nomer.ru/011/o/ru3019690.jpg\"\n      }\n    }\n  ]\n}",
    200,
    lp
  )

  private val emptyResponse = AvtonomerRawModel.apply(
    "{\n  \"error\": 1,\n  \"region\": \"763\",\n  \"informer\": null,\n  \"cars\": []\n}",
    200,
    lp
  )

  before {
    reset(avatarnica)
    reset(rawStorageManager)
    reset(unificator)

  }

  "AvtoNomerWorker" should {
    "process no result correctly" in {
      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvtonomerStateBuilder.setShouldProcess(true)
      when(avtonomerClient.getPhoto(?)(?, ?)).thenReturn(
        Future.successful(emptyResponse)
      )
      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future.successful(List.empty))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

      val holder = WatchingStateHolder(lp, stateBuilder.build(), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      verify(rawStorageManager, times(1)).upsert(?)(?)
      verify(avatarnica, never()).putImageFromUrl(?, ?, ?)(?)
      verify(unificator, never()).unifyHeadOption(?)(?)
      assert(update.state.getAvtonomerState.getNotFound)

      assert(update.delay.toDuration.toSeconds == 0)
      assert(update.delay.isFinite)
      assert(update.state.getAvtonomerState.getLastCheck != 0)
    }

    "process correct response" in {
      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvtonomerStateBuilder.setShouldProcess(true)

      when(avtonomerClient.getPhoto(?)(?, ?)).thenReturn(Future.successful(responseWithRecords))
      when(unificator.unifyHeadOption(?)(?)).thenReturn(Future.successful(None))
      when(avatarnica.putImageFromUrl(?, ?, ?)(?)).thenReturn(
        Future.successful(
          PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setGroupId(1).setName("name")).build()
        )
      )

      when(rawStorageManager.getAllPrepared(?, ?)(?)).thenReturn(Future.successful(List.empty))
      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

      val holder = WatchingStateHolder(lp, stateBuilder.build(), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      verify(rawStorageManager, times(1)).upsert(?)(?)
      verify(avatarnica, times(2)).putImageFromUrl(?, ?, ?)(?)
      verify(unificator, times(2)).unifyHeadOption(?)(?)

      assert(!update.state.getAvtonomerState.getNotFound)
      assert(update.delay.toDuration.toSeconds == 0)
      assert(update.delay.isFinite)
      assert(update.state.getAvtonomerState.getLastCheck != 0)
    }

    "reschedule cause of errors from avatarnica" in {
      val stateBuilder = CompoundState.newBuilder()
      stateBuilder.getAvtonomerStateBuilder.setShouldProcess(true)

      when(avtonomerClient.getPhoto(?)(?, ?)).thenReturn(Future.successful(responseWithRecords))
      when(unificator.unifyHeadOption(?)(?)).thenReturn(Future.successful(None))
      when(avatarnica.putImageFromUrl(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException("")))

      when(rawStorageManager.upsert(?)(?)).thenReturn(Future.unit)

      val holder = WatchingStateHolder(lp, stateBuilder.build(), System.currentTimeMillis())
      val res = worker.action(holder)
      val update = res.updater.get.apply(holder.toUpdate)

      verify(rawStorageManager, never()).upsert(?)(?)

      assert(!update.delay.isDefault)
      assert(update.delay.isFinite)
      assert(update.state.getAvtonomerState.getLastCheck == 0)
    }
  }

}
