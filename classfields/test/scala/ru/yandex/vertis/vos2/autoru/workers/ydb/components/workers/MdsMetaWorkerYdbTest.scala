package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.google.common.util.concurrent.RateLimiter
import org.mockito.Mockito.{doNothing, never, times, verify}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata.NeuralNetClass
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo.Builder
import ru.yandex.vos2.BasicsModel.PhotoMetadata
import ru.yandex.vos2.autoru.utils.mds.MdsPhoto
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.commonfeatures.VosFeatureTypes.{VosFeature, WithGeneration}
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import ru.yandex.vos2.services.pica.PicaPicaClient
import ru.yandex.vos2.services.pica.PicaPicaClient._
import ru.yandex.vos2.util.lang.StringSlice
import ru.yandex.vos2.{getNow, BasicsModel, OfferModel}

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.vos.BlockedPhotoHashModel.BlockedPhotoHash
import ru.yandex.vos2.autoru.dao.blockedphotohashes.BlockedPhotoHashesDao

@RunWith(classOf[JUnitRunner])
class MdsMetaWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  val picaPicaLimiter: RateLimiter = RateLimiter.create(1000)

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val feature = mock[VosFeature]
    when(mockedFeatureManager.LoadMdsMeta).thenReturn(feature)

    val serviceSpecific = mock[MdsPhoto]
    val mdsPhotoUtils = mock[MdsPhotoUtils]
    val picaPicaClient = mock[PicaPicaClient]
    val blockedPhotoHashesDao = mock[BlockedPhotoHashesDao]
    val offerName = "offer1"
    val imageName = "image1"

    val offer = OfferModel.Offer
      .newBuilder()
      .setOfferService(OfferModel.OfferService.OFFER_AUTO)
      .setTimestampUpdate(0)
      .setUserRef("ref")
      .setOfferID(offerName)
      .build()

    lazy val photoBuilder = BasicsModel.Photo
      .newBuilder()
      .setName(imageName + "-group1")
      .setIsMain(true)
      .setNamespace("autoru-vos")
      .setOrigNamespace("autoru-orig")
      .setOrder(1)
      .setCreated(getNow)
    lazy val photo = photoBuilder.build()
    val MarkClssificationId = 5

    def picaResponse: TaskResult

    when(feature.value).thenReturn(WithGeneration(false, 1))
    when(serviceSpecific.photoBuilders(any())).thenReturn(Seq(photoBuilder))
    when(serviceSpecific.photos(any())).thenReturn(Seq(photo))
    doNothing().when(serviceSpecific).fillServiceSpecific(?, ?)
    stub(serviceSpecific.picaPartitionId _) { case offer => offer.getUserRef }
    when(mdsPhotoUtils.getMdsInfo(any())).thenReturn(Some(("group1", imageName)))
    when(mdsPhotoUtils.getMainPhotoUrl(any())).thenReturn(Some(s"http://image/path/group1/$imageName/size_id"))
    stub(
      picaPicaClient.send(
        _: Seq[Task],
        _: String,
        _: String,
        _: Int,
        _: Option[Boolean],
        _: Option[Int]
      )(_: Traced)
    ) { case args => picaResponse }
    stub(mdsPhotoUtils.getMainUrl _) { case (namespace, group, name) => s"http://image/path/$group/$name/size_id" }
    when(blockedPhotoHashesDao.getHashes(any())(any())).thenReturn(List.empty)

    val worker =
      new MdsMetaWorkerYdb(serviceSpecific, mdsPhotoUtils, picaPicaClient, picaPicaLimiter, None)
        with YdbWorkerTestImpl {
        override def features: FeaturesManager = mockedFeatureManager
      }
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

      // setup auto_exterior_weight using service specific code
      stub(serviceSpecific.fillServiceSpecific _) {
        case (photo, metadata) =>
          metadata.getNeuralNetClassesList.asScala.find(_.getName == "broken") match {
            case Some(classification) =>
              // TODO
              photo.getAutoClassificationBuilder.setAutoBrokenWeight(classification.getWeight)
            case None =>
          }
      }

      def picaResponse: TaskResult =
        TaskResult(
          Map(
            offerName -> Map(imageName -> OkUpload("mds-group1", imageName, "autoru-vos", Metadata.V3(metadata)))
          )
        )

//      val state = asyncProcess(stage, offer)
      worker.process(offer, None).updateOfferFunc.get(offer)
      photoBuilder.getMeta.getAutoClassification.getAutoBrokenWeight shouldEqual 200
      photoBuilder.getMeta.getCvHash shouldEqual "AABBCCHH"
      photoBuilder.getMeta.getOrigSize shouldEqual PhotoMetadata.Size.newBuilder().setWidth(100).setHeight(150).build()
      assert(!photoBuilder.getMeta.hasSmartCrop, "SmartCrop must be empty")
    }

    "retry" when {
      "meta in not finished" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata.newBuilder().setIsFinished(false).setVersion(5).build()

        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> OkUpload("mds-group1", imageName, "autoru-vos", Metadata.V3(metadata)))
            )
          )

        val result = worker.process(offer, None)
//        val state = asyncProcess(stage, offer)
        assert(result.nextCheck.nonEmpty, "Offer processing must be retried in the nearest future")
      }

      "undefined error response status from MDS" in new Fixture {
        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> FailedUpload(errorMessage = Some("Some undefined error from MDS")))
            )
          )

        val result = worker.process(offer, None)
        assert(result.nextCheck.nonEmpty, "Offer processing must be retried in the nearest future")
      }

      "new meta retrieved" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata.newBuilder().setIsFinished(true).setVersion(5).build()

        override def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName ->
                Map(imageName -> OkUpload("mds-group1", imageName, "autoru-vos", Metadata.V3(metadata)))
            )
          )

        val result = worker.process(offer, None)
        assert(result.nextCheck.nonEmpty, "Offer processing must be retried in the nearest future")
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
          .setIsMain(true)
          .setOrder(1)
          .setCreated(getNow)
          .setMeta(metadata)

        override def picaResponse: TaskResult = fail("Should not be invoked")

        val result = worker.process(offer, None)
        assert(result.nextCheck.isEmpty, "Offer processing must be skipped")
      }

      "empty photo list" in new Fixture {
        when(serviceSpecific.photos(any())).thenReturn(Seq())
        when(serviceSpecific.photoBuilders(any())).thenReturn(Seq())

        override def picaResponse: TaskResult = fail("Should not be invoked")

        val result = worker.process(offer, None)
        assert(result.nextCheck.isEmpty, "Offer processing must be skipped")
      }

      "photos was not found in MDS before" in new Fixture {
        lazy val metadata = PicaPicaSchema.Metadata
          .newBuilder()
          .setIsFinished(true)
          .setVersion(5)
          .build()

        val tempPhotoBuilder = photo.toBuilder
        tempPhotoBuilder
          .addPhotoCheckExistCacheBuilder()
          .setName(tempPhotoBuilder.getName)
          .setNamespace(tempPhotoBuilder.getNamespace)
          .setNotFound(true)
        when(serviceSpecific.photos(any())).thenReturn(Seq(tempPhotoBuilder.build()))

        def picaResponse: TaskResult =
          TaskResult(
            Map(
              offerName -> Map(imageName -> OkUpload("mds-group1", imageName, "autoru-vos", Metadata.V3(metadata)))
            )
          )

        val result = worker.process(offer, None)
        assert(result.nextCheck.isEmpty, "Offer processing must be skipped")

      }
    }

    "fetch meta using original photos" in new Fixture {
      override lazy val photoBuilder: Builder = offer.toBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("group1-blured-image")
        .setOrigName("group2-original-image")
        .setOrigNamespace("autoru-orig")
        .setNamespace("autoru-vos")
        .setIsMain(false)
        .setOrder(2)
        .setCreated(getNow)

      lazy val metadata = PicaPicaSchema.Metadata
        .newBuilder()
        .setIsFinished(true)
        .setVersion(5)
        .setGlobalSemidupDescriptor64("AABBCCHH_ORIGINAL")
        .build()

      override def picaResponse: TaskResult = null.asInstanceOf[TaskResult] // should not be used in this test

      stub(mdsPhotoUtils.getMdsInfo _) { case name => StringSlice.split2('-').unapply(name) }
      when(mdsPhotoUtils.getMainPhotoUrl(any())).thenReturn(Some("http://image/path/group1/blured-image"))
      stub(
        picaPicaClient.send(
          _: Seq[Task],
          _: String,
          _: String,
          _: Int,
          _: Option[Boolean],
          _: Option[Int]
        )(_: Traced)
      ) {
        case (task :: _, _, _, _, _, _, _) =>
          task.urls should have size 1
          task.urls.head.imageId shouldEqual "original-image"
          task.urls.head.srcUrl shouldEqual "http://image/path/group2/original-image/size_id"
          TaskResult(
            Map(
              task.offerKey ->
                task.urls
                  .map(d => d.imageId -> OkUpload("group2", d.imageId, "autoru-orig", Metadata.V3(metadata)))
                  .toMap
            )
          )
      }

      worker.process(offer, None).updateOfferFunc.get(offer)

      photoBuilder.getMeta.getVersion shouldEqual 5
      photoBuilder.getMeta.getCvHash shouldEqual "AABBCCHH_ORIGINAL"
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

      override val worker = new MdsMetaWorkerYdb(
        serviceSpecific,
        mdsPhotoUtils,
        picaPicaClient,
        limiter,
        Some(priorityLimiter)
      ) with YdbWorkerTestImpl {
        override def features: FeaturesManager = mockedFeatureManager
      }

      worker.process(offer.toBuilder.setTimestampCreate(0).build(), None)

      picaInvocationsCounter shouldEqual 1

      worker.process(offer.toBuilder.setTimestampCreate(0).build(), None)

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

      override val worker = new MdsMetaWorkerYdb(
        serviceSpecific,
        mdsPhotoUtils,
        picaPicaClient,
        limiter,
        Some(priorityLimiter)
      ) with YdbWorkerTestImpl {
        override def features: FeaturesManager = mockedFeatureManager
      }

      val newOffer = offer.toBuilder.setTimestampCreate(getNow).build()

      worker.process(newOffer, None)

      picaInvocationsCounter shouldEqual 1
      verify(priorityLimiter).tryAcquire()
      verify(limiter, never()).tryAcquire()

      worker.process(newOffer, None)

      picaInvocationsCounter shouldEqual 2
      verify(priorityLimiter, times(2)).tryAcquire()
      verify(limiter, times(1)).tryAcquire()

      worker.process(newOffer, None)

      picaInvocationsCounter shouldEqual 2
      verify(priorityLimiter, times(3)).tryAcquire()
      verify(limiter, times(2)).tryAcquire()
    }
  }
}
