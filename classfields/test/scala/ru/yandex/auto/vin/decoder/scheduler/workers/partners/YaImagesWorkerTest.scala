package ru.yandex.auto.vin.decoder.scheduler.workers.partners

import auto.carfax.common.clients.hamster.HamsterClient
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.{PhotoEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.storage.licenseplate.LicensePlateWatchingDao
import auto.carfax.common.utils.time.Clock
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, OnlyPreparedModel, PreparedData}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class YaImagesWorkerTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfterEach {

  import YaImagesWorkerTest._

  val hamster: HamsterClient = mock[HamsterClient]
  val rsm: RawStorageManager[LicensePlate] = mock[RawStorageManager[LicensePlate]]
  val feature: Feature[Boolean] = Feature("", _ => true)
  val rl: RateLimiter = mock[RateLimiter]
  val lpUpdateDao: LicensePlateWatchingDao = mock[LicensePlateWatchingDao]
  val queue: WorkersQueue[LicensePlate, CompoundState] = mock[WorkersQueue[LicensePlate, CompoundState]]
  val clock: Clock = mock[Clock]

  implicit val t: Traced = Traced.empty
  implicit private val metrics = TestOperationalSupport
  implicit val tracer = NoopTracerFactory.create()

  val worker = new YaImagesWorker(hamster, rsm, rl, feature, lpUpdateDao, queue, clock)

  override def beforeEach() = {
    reset(hamster)
    reset(rsm)
    reset(queue)
    reset(rl)
    reset(lpUpdateDao)
    when(clock.millis).thenReturn(System.currentTimeMillis())
    ()
  }

  "action" should {
    "ignore when no ya_images" in {
      val b = CompoundState.newBuilder().build()

      intercept[IllegalArgumentException] {
        worker.action(WatchingStateHolder(lp, b, 1))
      }
    }

    "delay process when rate limit exceeded" in {
      when(rl.tryAcquire(?, ?, ?)).thenReturn(false)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)

      val res = worker.action(WatchingStateHolder(lp, b.build, 1))
      val updated = res.updater.get(holder.toUpdate)

      res.updater.get.delay().isDefault shouldBe false
      updated.state.getYaImagesState.getShouldProcess shouldBe true
    }

    "delay process when process failed" in {
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.failed(new RuntimeException))

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)

      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      res.updater.get.delay().isDefault shouldBe false
      updated.state.getYaImagesState.getShouldProcess shouldBe true
    }

    "no process when notFound = true in raw storage" in {
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          preparedFromPhotoInfo(
            photoInfoWithBuilder(_.setNotFound(true))
          )
        )
      )
      when(rsm.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)
      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      verify(hamster, never()).isImageExistsInIndex(?)
      res.updater.get.delay().toDuration.toSeconds shouldBe 0
      updated.state.getYaImagesState.getShouldProcess shouldBe false
    }

    "delay when storage update is failed" in {
      val now = System.currentTimeMillis()
      when(clock.millis).thenReturn(now)
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          preparedFromPhotoInfo(
            photoInfoWithBuilder(identity)
          )
        )
      )
      when(hamster.isImageExistsInIndex(?)).thenReturn(Future.successful(true))
      when(rsm.updatePrepared(?, ?, ?)(?)).thenThrow(new RuntimeException)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)

      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      val expectedPreparedData = preparedFromPhotoInfo(
        photoInfoWithBuilder(_.setMeta(metaDataB.setYaSearch(yaSearchB.setLastCheck(now))))
      )
      verify(rsm, times(1)).updatePrepared(eq(lp), eq(expectedPreparedData.head.prepared.data), ?)(?)
      !res.updater.get.delay().isDefault shouldBe true
      updated.state.getYaImagesState.getShouldProcess shouldBe true
    }

    "no process when lastCheck < day" in {
      val now = System.currentTimeMillis()
      val elapsedTime = 60.minutes.toMillis

      when(clock.millis).thenReturn(now)
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          preparedFromPhotoInfo(
            photoInfoWithBuilder(
              _.setMeta(
                metaDataB
                  .setYaSearch(
                    yaSearchB
                      .setLastCheck(now + elapsedTime)
                  )
              )
            )
          )
        )
      )
      when(rsm.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)
      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      verify(hamster, never()).isImageExistsInIndex(?)
      verify(rsm, never()).updatePrepared(?, ?, ?)(?)
      res.updater.get.delay().toDuration.toSeconds shouldBe 0
      updated.state.getYaImagesState.getShouldProcess shouldBe false
    }

    "update photo info correctly when hamster returns false" in {
      updatePreparedCorrectly(hamsterResult = false)
    }

    "update photo info correctly when hamster returns true" in {
      updatePreparedCorrectly(hamsterResult = true)
    }

    "check in hamster if isDeleted = true anyway and update correctly" in {
      val now = System.currentTimeMillis()
      when(clock.millis).thenReturn(now)
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          preparedFromPhotoInfo(
            photoInfoWithBuilder(_.setIsDeleted(true))
          )
        )
      )
      when(hamster.isImageExistsInIndex(?)).thenReturn(Future.successful(true))
      when(rsm.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)

      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      val expectedPreparedData = preparedFromPhotoInfo(
        photoInfoWithBuilder(
          _.setIsDeleted(false).setMeta(
            metaDataB
              .setYaSearch(yaSearchB.setLastCheck(now))
          )
        )
      )
      verify(rsm, times(1)).updatePrepared(eq(lp), eq(expectedPreparedData.head.prepared.data), ?)(?)
      res.updater.get.delay().toDuration.toSeconds shouldBe 0
      updated.state.getYaImagesState.getShouldProcess shouldBe false
      updated.state.getYaImagesState.getNotFound shouldBe false
    }

    def updatePreparedCorrectly(hamsterResult: Boolean) = {
      val now = System.currentTimeMillis()
      when(clock.millis).thenReturn(now)
      when(rl.tryAcquire(?, ?, ?)).thenReturn(true)
      when(rsm.getMetaAndPrepared(?, ?)(?)).thenReturn(
        Future.successful(
          preparedFromPhotoInfo(
            photoInfoWithBuilder(identity)
          )
        )
      )
      when(hamster.isImageExistsInIndex(?)).thenReturn(Future.successful(hamsterResult))
      when(rsm.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)

      val b = CompoundState.newBuilder()
      b.setYaImagesState(b.getYaImagesStateBuilder.setShouldProcess(true))
      val holder = WatchingStateHolder(lp, b.build, 1)

      val res = worker.action(holder)
      val updated = res.updater.get(holder.toUpdate)

      val expectedPreparedData = preparedFromPhotoInfo(
        photoInfoWithBuilder(
          _.setIsDeleted(!hamsterResult).setMeta(
            metaDataB
              .setYaSearch(yaSearchB.setLastCheck(now))
          )
        )
      )
      verify(rsm, times(1)).updatePrepared(eq(lp), eq(expectedPreparedData.head.prepared.data), ?)(?)
      res.updater.get.delay().toDuration.toSeconds shouldBe 0
      updated.state.getYaImagesState.getShouldProcess shouldBe false
    }

  }
}

object YaImagesWorkerTest {
  val lp = LicensePlate("o000oo116")

  def photoInfoWithBuilder(b: PhotoInfo.Builder => PhotoInfo.Builder): PhotoInfo = {
    val updated = b(
      PhotoInfo
        .newBuilder()
    )

    updated
      .setExternalPhotoUrl("http://yandex.ru/some_image.jpg")
      .build
  }

  val meta = MetaData("", EventType.YANDEX_IMAGES, "", 0L, 0L, 0L)

  def preparedFromPhotoInfo(photoInfo: PhotoInfo*) = Seq(
    OnlyPreparedModel(lp, PreparedData(historyFrom(photoInfo)), meta)
  )

  def historyFrom(photos: Seq[PhotoInfo]): VinInfoHistory =
    VinInfoHistory.newBuilder().addPhotoEvents(PhotoEvent.newBuilder().addAllImages(photos.asJava)).build

  val metaDataB = PhotoInfo.MetaData.newBuilder
  val yaSearchB = PhotoInfo.MetaData.YaSearch.newBuilder
}
