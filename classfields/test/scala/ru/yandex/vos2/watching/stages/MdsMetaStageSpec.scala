package ru.yandex.vos2.watching.stages

import com.google.common.util.concurrent.RateLimiter
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata.NeuralNetClass
import ru.yandex.realty.util.getNow
import ru.yandex.vos2.BasicsModel.{Photo, PhotoMetadata}
import ru.yandex.vos2.features.Feature
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import ru.yandex.vos2.services.pica.PicaPicaClient
import ru.yandex.vos2.services.pica.PicaPicaClient._
import ru.yandex.vos2.testing.meta.TestingMetaLoadingList
import ru.yandex.vos2.util.StageUtils
import ru.yandex.vos2.watching.mds.MdsPhoto
import ru.yandex.vos2.{BasicsModel, OfferModel}
import scala.concurrent.duration.DurationInt

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class MdsMetaStageSpec extends WordSpec with Matchers with MockitoSupport with StageUtils {
  val picaPicaLimiter: RateLimiter = RateLimiter.create(1000)

  private val whiteList: TestingMetaLoadingList = new TestingMetaLoadingList {
    override def isUidAllowed(uid: String): Boolean = true
  }

  abstract class Fixture {
    val feature = mock[Feature]
    val serviceSpecific = mock[MdsPhoto]
    val mdsPhotoUtils = mock[MdsPhotoUtils]
    val picaPicaClient = mock[PicaPicaClient]
    val offerName = "offer1"
    val imageName = "image1"

    val offer = OfferModel.Offer
      .newBuilder()
      .setOfferService(OfferModel.OfferService.OFFER_AUTO)
      .setTimestampUpdate(0)
      .setUserRef("ref")
      .build()
    lazy val photoBuilder: BasicsModel.Photo.Builder = BasicsModel.Photo
      .newBuilder()
      .setName(imageName + "-group1")
      .setCreated(getNow)
    lazy val photo = photoBuilder.build()
    val MarkClssificationId = 5

    def picaResponse: TaskResult

    when(feature.generation).thenReturn(1)
    when(serviceSpecific.photoBuilders(any())).thenReturn(Seq(photoBuilder))
    when(serviceSpecific.photos(any())).thenReturn(Seq(photo))
    doNothing().when(serviceSpecific).fillServiceSpecific(?, ?)
    stub(serviceSpecific.picaPartitionId _) { case offer ⇒ offer.getUserRef }
    when(mdsPhotoUtils.getMdsInfo(any())).thenReturn(Some(("group1", imageName)))
    when(mdsPhotoUtils.getMainPhoto(any())).thenReturn(Some(s"http://image/path/group1/$imageName/size_id"))
    stub(picaPicaClient.send _) { case args ⇒ picaResponse }
    stub(mdsPhotoUtils.getMainUrl _) { case (group, name) ⇒ s"http://image/path/$group/$name/size_id" }

    val stage =
      new MdsMetaStage(
        feature,
        serviceSpecific,
        mdsPhotoUtils,
        picaPicaClient,
        picaPicaLimiter,
        None,
        ProviderAdapter.create(whiteList)
      )
  }

  case class TestNeuralClasses(id: Int, weight: Int)

  "MdsMetaStageSpec" should {
    "fetch and apply metadata" in new Fixture {
      // metadata with classification "mark" weighted as 200
      lazy val metadata = PicaPicaSchema.Metadata
        .newBuilder()
        .setIsFinished(true)
        .setVersion(5)
        .setGlobalSemidupDescriptor64("AABBCCHH")
        .setOrigSize(PicaPicaSchema.Metadata.Size.newBuilder().setX(100).setY(150))
        .addNeuralNetClasses(NeuralNetClass.newBuilder().setName("broken").setWeight(200))
        .build()

      def picaResponse: TaskResult =
        TaskResult(
          Map(
            offerName -> Map(imageName -> OkUpload("mds-group1", imageName, Metadata.V3(metadata)))
          )
        )

      val state = asyncProcess(stage, offer)

      photoBuilder.getMeta.getCvHash shouldEqual "AABBCCHH"
      photoBuilder.getMeta.getOrigSize shouldEqual PhotoMetadata.Size.newBuilder().setWidth(100).setHeight(150).build()
      photoBuilder.getMeta.getIsFinished shouldBe true
      assert(!photoBuilder.getMeta.hasSmartCrop, "SmartCrop must be empty")

      // implementation details
      photoBuilder.hasMetaFailureState shouldBe false
    }

    "retry" when {
      "meta is not finished" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata.newBuilder().setIsFinished(false).setVersion(5).build()

        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> OkUpload("mds-group1", imageName, Metadata.V3(metadata)))
            )
          )

        val state = asyncProcess(stage, offer)
        assert(state.delay.isFinite, "Offer processing must be retried in the nearest future")

        photoBuilder.getMeta.getIsFinished shouldBe false
        photoBuilder.hasNewMeta shouldBe false

        // implementation details
        photoBuilder.hasMetaFailureState shouldBe true
        photoBuilder.getMetaFailureState.getMetaFetchAttemptCount shouldBe 1
        state.delay.toSeconds should be <= 60L
      }

      "undefined error response status from MDS" in new Fixture {
        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> FailedUpload(errorMessage = Some("Some undefined error from MDS")))
            )
          )

        val state = asyncProcess(stage, offer)
        assert(state.delay.isFinite, "Offer processing must be retried in the nearest future")

        photoBuilder.getMeta.getIsFinished shouldBe false
        photoBuilder.hasNewMeta shouldBe false

        // implementation details
        photoBuilder.hasMetaFailureState shouldBe true
        photoBuilder.getMetaFailureState.getMetaFetchAttemptCount shouldBe 1
        state.delay.toSeconds should be <= 60L
      }

      "new meta retrieved" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata.newBuilder().setIsFinished(true).setVersion(5).build()

        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> OkUpload("mds-group1", imageName, Metadata.V3(metadata)))
            )
          )

        val state = asyncProcess(stage, offer)
        assert(state.delay.isFinite, "Offer processing must be retried in the nearest future")

        photoBuilder.getMeta.getIsFinished shouldBe true

        // implementation details
        photoBuilder.hasMetaFailureState shouldBe false
        state.delay.toSeconds should be < 10L
      }
    }

    "skip" when {
      "no new meta retrieved" in new Fixture {
        lazy val metadata = BasicsModel.PhotoMetadata
          .newBuilder()
          .setIsFinished(true)
          .setVersion(5)
          .build()
        override lazy val photoBuilder = BasicsModel.Photo
          .newBuilder()
          .setName(imageName + "-group1")
          .setCreated(getNow)
          .setMeta(metadata)

        override def picaResponse: TaskResult = fail("Should not be invoked")

        val state = asyncProcess(stage, offer)
        assert(!state.delay.isFinite, "Offer processing must be skipped")
      }

      "empty photo list" in new Fixture {
        when(serviceSpecific.photos(any())).thenReturn(Seq())
        when(serviceSpecific.photoBuilders(any())).thenReturn(Seq())

        override def picaResponse: TaskResult = fail("Should not be invoked")

        val state = asyncProcess(stage, offer)
        assert(!state.delay.isFinite, "Offer processing must be skipped")
      }

      "photos not found in MDS" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata.newBuilder().setIsFinished(true).setVersion(5).build()

        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> FailedUpload(errorMessage = Some("Not Found")))
            )
          )

        val tempPhotoBuilder = photo.toBuilder
        when(serviceSpecific.photos(any())).thenReturn(Seq(tempPhotoBuilder.build()))
        when(serviceSpecific.photoBuilders(any())).thenReturn(Seq(tempPhotoBuilder))

        val state = asyncProcess(stage, offer)
        assert(!state.delay.isFinite(), "Should not schedule retries when Pica replies with Not Found")
        assert(tempPhotoBuilder.getNotFoundInMds)

        // implementation details
        tempPhotoBuilder.hasMetaFailureState shouldBe false
      }

      "photos was not found in MDS before" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata
          .newBuilder()
          .setIsFinished(true)
          .setVersion(5)
          .build()

        val tempPhotoBuilder = photo.toBuilder
        tempPhotoBuilder.setNotFoundInMds(true)
        when(serviceSpecific.photos(any())).thenReturn(Seq(tempPhotoBuilder.build()))

        def picaResponse: TaskResult = fail("Should not be invoked")

        val state = asyncProcess(stage, offer)
        assert(!state.delay.isFinite, "Offer processing must be skipped")
      }
    }

    "respect exponential backoff" when {
      "enough time has passed since previous attempt" in new Fixture {
        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> FailedUpload(errorMessage = Some("Some undefined error from MDS")))
            )
          )

        val attemptTs: Long = getNow - 61.seconds.toMillis
        photoBuilder.getMetaFailureStateBuilder
          .setMetaFetchAttemptCount(1)
          .setMetaFetchLastAttemptTimestamp(attemptTs)

        val state = asyncProcess(stage, offer)
        assert(state.delay.isFinite, "Offer processing must be retried in the nearest future")

        photoBuilder.getMeta.getIsFinished shouldBe false
        photoBuilder.hasNewMeta shouldBe false

        // implementation details
        photoBuilder.hasMetaFailureState shouldBe true
        photoBuilder.getMetaFailureState.getMetaFetchAttemptCount shouldBe 2
        photoBuilder.getMetaFailureState.getMetaFetchLastAttemptTimestamp should be > attemptTs
        state.delay.toSeconds should be > 60L
        state.delay.toSeconds should be <= 120L
      }

      "it is too early to retry" in new Fixture {
        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> FailedUpload(errorMessage = Some("Some undefined error from MDS")))
            )
          )

        val attemptTs: Long = getNow - 20.seconds.toMillis
        photoBuilder.getMetaFailureStateBuilder
          .setMetaFetchAttemptCount(1)
          .setMetaFetchLastAttemptTimestamp(attemptTs)

        val state = asyncProcess(stage, offer)
        assert(state.delay.isFinite, "Offer processing must be retried in the nearest future")

        photoBuilder.getMeta.getIsFinished shouldBe false
        photoBuilder.hasNewMeta shouldBe false

        // implementation details
        photoBuilder.hasMetaFailureState shouldBe true
        photoBuilder.getMetaFailureState.getMetaFetchAttemptCount shouldBe 1
        photoBuilder.getMetaFailureState.getMetaFetchLastAttemptTimestamp shouldBe attemptTs
        state.delay.toSeconds should be <= 40L
      }
    }

    "use limiter for regular offers" in new Fixture {
      var picaInvocationsCounter = 0
      val limiter = mock[RateLimiter]
      when(limiter.tryAcquire()).thenReturn(true).thenReturn(false)
      val priorityLimiter = mock[RateLimiter]

      override def picaResponse: TaskResult = {
        picaInvocationsCounter += 1
        TaskResult(Map.empty)
      }

      override val stage: MdsMetaStage =
        new MdsMetaStage(
          feature,
          serviceSpecific,
          mdsPhotoUtils,
          picaPicaClient,
          limiter,
          Some(priorityLimiter),
          ProviderAdapter.create(whiteList)
        )

      asyncProcess(stage, offer.toBuilder.setTimestampCreate(0).build())

      picaInvocationsCounter shouldEqual 1

      asyncProcess(stage, offer.toBuilder.setTimestampCreate(0).build())

      picaInvocationsCounter shouldEqual 1

      verify(limiter, times(2)).tryAcquire()
      verify(priorityLimiter, never()).tryAcquire()
    }

    "use priority limiter for priority offers" in new Fixture {
      var picaInvocationsCounter = 0
      val limiter = mock[RateLimiter]
      when(limiter.tryAcquire()).thenReturn(true).thenReturn(false)
      val priorityLimiter = mock[RateLimiter]
      when(priorityLimiter.tryAcquire()).thenReturn(true).thenReturn(false).thenReturn(false)

      override def picaResponse: TaskResult = {
        picaInvocationsCounter += 1
        TaskResult(Map.empty)
      }

      override val stage: MdsMetaStage =
        new MdsMetaStage(
          feature,
          serviceSpecific,
          mdsPhotoUtils,
          picaPicaClient,
          limiter,
          Some(priorityLimiter),
          ProviderAdapter.create(whiteList)
        )

      val newOffer = offer.toBuilder.setTimestampCreate(getNow).build()

      asyncProcess(stage, newOffer)

      picaInvocationsCounter shouldEqual 1
      verify(priorityLimiter).tryAcquire()
      verify(limiter, never()).tryAcquire()

      asyncProcess(stage, newOffer)

      picaInvocationsCounter shouldEqual 2
      verify(priorityLimiter, times(2)).tryAcquire()
      verify(limiter, times(1)).tryAcquire()

      asyncProcess(stage, newOffer)

      picaInvocationsCounter shouldEqual 2
      verify(priorityLimiter, times(3)).tryAcquire()
      verify(limiter, times(2)).tryAcquire()
    }
  }
}
