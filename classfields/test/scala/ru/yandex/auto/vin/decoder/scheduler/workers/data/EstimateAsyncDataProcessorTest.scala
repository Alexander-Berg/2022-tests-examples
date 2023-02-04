package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Estimate, VinInfoHistory}
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, OnlyPreparedModel, PreparedData}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.IterableHasAsJava

class EstimateAsyncDataProcessorTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfter {

  implicit val t: Traced = Traced.empty
  private val vin = VinCode("X4X3D59430PS96744")
  private val rawStorageManager = mock[RawStorageManager[VinCode]]
  private val photoManager = mock[AvatarsPhotoManager]
  val processor = new EstimatesAsyncDataProcessor(rawStorageManager, photoManager)
  val delay = DefaultDelay(1.hour)

  before {
    reset(rawStorageManager)
    reset(photoManager)
    when(photoManager.prepareUrl(?)).thenCallRealMethod()
    when(photoManager.isImageProcessed(?)).thenCallRealMethod()
  }

  "process" should {
    "do not update records" when {
      "there are not records in storage" in {
        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List.empty))
        when(photoManager.tryEnrichImagesWithMdsInfo(List.empty)).thenReturn(Future.successful(List.empty))

        val state = createPreparedDataState(EventType.ACAT_INFO, true)
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?)
      }
      "all images already processed" in {

        val photo1 = buildPhoto(
          "photo1",
          false,
          Some(MdsPhotoInfo.newBuilder().setGroupId(1).setName("photo1").setNamespace("autoru-carfax").build())
        )
        val photo2 = buildPhoto("photo2", true, None)
        val record = buildRecord(List(photo1, photo2))

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(photoManager.tryEnrichImagesWithMdsInfo(List(photo1, photo2)))
          .thenReturn(Future.successful(List(photo1, photo2)))

        val state = createPreparedDataState(EventType.ACAT_INFO, true)
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, never()).updatePrepared(?, ?, ?)(?)
      }
    }
    "update records and finish" when {
      "all images processed after processing" in {
        val photo1 = buildPhoto(
          "photo1",
          false,
          Some(MdsPhotoInfo.newBuilder().setGroupId(1).setName("photo1").setNamespace("autoru-carfax").build())
        )
        val photo2 = buildPhoto("photo2", false, None)
        val updatedPhoto2 = buildPhoto("photo2", true, None)

        val record = buildRecord(List(photo1, photo2))
        val updatedPrepared = buildVh(List(photo1, updatedPhoto2))

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(photoManager.tryEnrichImagesWithMdsInfo(List(photo1, photo2)))
          .thenReturn(Future.successful(List(photo1, updatedPhoto2)))

        val state = createPreparedDataState(EventType.ACAT_INFO, true)
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe false

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
    }
    "update records and reschedule" when {
      "not all images processed after processing" in {

        val photo1 = buildPhoto("photo1", false, None)
        val photo2 = buildPhoto("photo2", false, None)

        val updatedPhoto1 = buildPhoto(
          "photo1",
          false,
          Some(MdsPhotoInfo.newBuilder().setGroupId(1).setName("photo1").setNamespace("autoru-carfax").build())
        )

        val record = buildRecord(List(photo1, photo2))
        val updatedPrepared = buildVh(List(updatedPhoto1, photo2))

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.unit)
        when(photoManager.tryEnrichImagesWithMdsInfo(List(photo1, photo2)))
          .thenReturn(Future.successful(List(updatedPhoto1, photo2)))

        val state = createPreparedDataState(EventType.ACAT_INFO, true)
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe true

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
      "update failed" in {

        val photo1 = buildPhoto("photo1", false, None)
        val updatedPhoto1 = buildPhoto("photo1", true, None)

        val record = buildRecord(List(photo1))
        val updatedPrepared = buildVh(List(updatedPhoto1))

        when(rawStorageManager.getMetaAndPrepared(?, ?)(?)).thenReturn(Future.successful(List(record)))
        when(rawStorageManager.updatePrepared(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException("")))
        when(photoManager.tryEnrichImagesWithMdsInfo(List(photo1)))
          .thenReturn(Future.successful(List(updatedPhoto1)))

        val state = createPreparedDataState(EventType.ACAT_INFO, true)
        val compoundState = CompoundState.newBuilder().addPreparedDataState(state).build()

        val res = processor.process(vin, state).await

        res.updater.isEmpty shouldBe false
        val updated = res.updater.get.apply(WatchingStateUpdate(compoundState, delay))
        updated.state.getPreparedDataStateCount shouldBe 1
        updated.state.getPreparedDataState(0).getShouldProcess shouldBe true

        verify(rawStorageManager, times(1)).updatePrepared(?, eq(updatedPrepared), ?)(?)
      }
    }
  }

  private def createPreparedDataState(eventType: EventType, shouldProcess: Boolean): PreparedDataState = {
    PreparedDataState.newBuilder().setEventType(eventType).setShouldProcess(shouldProcess).build()
  }

  private def buildRecord(photo: List[PhotoInfo]): OnlyPreparedModel[VinCode] = {
    OnlyPreparedModel(
      vin,
      PreparedData(buildVh(photo)),
      MetaData("", EventType.AAA_MOTORS_INSURANCE, "", 123L, 123L, 123L)
    )
  }

  private def buildVh(photo: List[PhotoInfo]) = {
    val vh = VinInfoHistory.newBuilder()
    val estimate = Estimate.newBuilder()
    estimate.addAllImages(photo.asJava)
    vh.addEstimates(estimate)
    vh.build()
  }

  private def buildPhoto(url: String, notFound: Boolean, mdsInfo: Option[MdsPhotoInfo]): PhotoInfo = {
    val builder = PhotoInfo.newBuilder().setExternalPhotoUrl(url).setNotFound(notFound)
    mdsInfo.foreach(builder.setMdsPhotoInfo)
    builder.build()
  }

}
