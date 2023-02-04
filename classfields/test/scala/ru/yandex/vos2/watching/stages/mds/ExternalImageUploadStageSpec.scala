package ru.yandex.vos2.watching.stages.mds

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.picapica.PicaUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.picapica.client.PicaPicaClient
import ru.yandex.vertis.picapica.model.{Metadata, TaskResult}
import ru.yandex.vos2.{BasicsModel, OfferModel}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.config.TestRealtySchedulerComponents
import ru.yandex.vos2.model.CommonGen.ShortEngStringGen
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.realty.model.offer.YoutubeUrlGenerator.YoutubeUrlGen
import ru.yandex.vos2.watching.ProcessingState
import ru.yandex.vos2.watching.stages.mds.ExternalImageUploadStageSpec._

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ExternalImageUploadStageSpec extends WordSpec with Matchers with MockitoSupport with PropertyChecks {

  val components = new TestRealtySchedulerComponents
  val picaClientMock = mock[PicaPicaClient]
  implicit val ec = components.ec

  val processor = new ExternalImageProcessor(picaClientMock, 5.seconds)(TestOperationalSupport)

  val stage =
    new ExternalImageUploadStage(None, processor, components.coreComponents.mdsPhotoUtils, RealtyMdsPhoto)

  "ExternalImageUploadStage" should {

    "download image and store all of it in the same order" in {
      forAll(
        RealtyOfferGenerator.offerGen(),
        ShortEngStringGen.generate(7),
        YoutubeUrlGen,
        ShortEngStringGen.generate(12)
      ) { (offerGen, url1, url2, url3) =>
        // step 1
        val mapKeys = Seq(url1, url2, url3)
        val imgUrls = Map(url1 -> true, url2 -> false, url3 -> true)
        val offer = offerGen.toBuilder
          .initOfferWithImageUrls(imgUrls)
          .build
        val offerId = offer.getExternalId
        mockPicaClientWithImageUrls(offerId, imgUrls)

        val r = stage.process(ProcessingState(offer, offer))
        r.offer.getOfferRealty.getPhotoCount shouldBe imgUrls.size
        r.offer.getOfferRealty.getPhotoList.asScala
          .map(_.getExternalUrl) should contain theSameElementsInOrderAs mapKeys

        // step2
        val mapKeysNewOrder = Seq(url3, url1, url2)
        val imgUrlsNewOrder = Map(url3 -> true, url1 -> true, url2 -> true)
        // rm first photo
        val newPhotoSeq = r.offer.getOfferRealty.getPhotoList.asScala.filterNot { x =>
          x.getExternalUrl == imgUrlsNewOrder.keys.head
        }
        val newOffer =
          r.offer.toBuilder
            .setOfferRealty(
              r.offer.getOfferRealty.toBuilder.clearPhoto().addAllPhoto(newPhotoSeq.toIterable.asJava)
            )
            // change order of imgRefs
            .clearImageRef()
            .initOfferWithImageUrls(imgUrlsNewOrder)
            .build()
        val r1 = stage.process(ProcessingState(newOffer, newOffer))
        r1.offer.getOfferRealty.getPhotoCount shouldBe imgUrlsNewOrder.size
        r1.offer.getOfferRealty.getPhotoList.asScala
          .map(_.getExternalUrl) should contain theSameElementsInOrderAs mapKeysNewOrder
      }
    }
  }

  "ExternalImageUploadStage" should {
    "download 2 image, 1 fail and then download 1 failed img and store all of it in the same order" in {
      forAll(
        RealtyOfferGenerator.offerGen(),
        ShortEngStringGen.generate(7),
        YoutubeUrlGen,
        ShortEngStringGen.generate(12)
      ) { (offerGen, url1, url2, url3) =>
        val imgUrls = Map(url1 -> true, url2 -> false, url3 -> true)
        val offer = offerGen.toBuilder
          .initOfferWithImageUrls(imgUrls)
          .build
        val offerId = offer.getExternalId
        mockPicaClientWithImageUrls(offerId, imgUrls)
        val r = stage.process(ProcessingState(offer, offer))
        r.offer.getOfferRealty.getPhotoCount shouldBe imgUrls.size

        r.offer.getOfferRealty.getPhotoList.asScala.count(!_.getMdsUploadFailed) shouldBe imgUrls
          .count(_._2)
        r.offer.getOfferRealty.getPhotoList.asScala
          .filter(_.getMdsUploadFailed)
          .map(_.getExternalUrl)
          .headOption shouldBe imgUrls.filterNot(_._2).keys.headOption

        mockPicaClientWithImageUrls(offerId, Map(url2 -> true))
        val r2 = stage.process(ProcessingState(r.offer, r.offer))

        r2.offer.getOfferRealty.getPhotoList.size shouldBe imgUrls.size
        r2.offer.getOfferRealty.getPhotoList.asScala
          .map(_.getExternalUrl) should contain theSameElementsInOrderAs imgUrls.keys
      }
    }
  }

  private def mockPicaClientWithImageUrls(offerId: String, urls: Map[String, Boolean]): Unit = {
    val m =
      Map(PicaUtils.getOfferKey(isFromFeed = true, offerId, ???) -> urls.map {
        case (url, isSuccessful) =>
          PicaUtils.imageHash(url) -> {
            if (isSuccessful)
              ru.yandex.vertis.picapica.model.UploadInfo
                .OkUpload("group1", "name", Metadata.apply(Map[String, String]()))
            else ru.yandex.vertis.picapica.model.UploadInfo.FailedUpload(None)
          }
      }.toMap)
    when(picaClientMock.send(?, ?, ?, ?, ?)).thenReturn(Future(TaskResult(m)))
  }
}

object ExternalImageUploadStageSpec {
  implicit class OfferUpdate(offer: OfferModel.Offer.Builder) {

    def initOfferWithImageUrls(urls: Map[String, Boolean]): OfferModel.Offer.Builder =
      offer
        .clearImageRef()
        .addAllImageRef(
          urls.keys.map(OfferModel.ImageRef.newBuilder().setUrl(_).setActive(true).build()).asJava
        )

    def initOfferWithPhotos(offer: Offer, urls: Map[String, Boolean]): OfferModel.Offer.Builder =
      offer.toBuilder
        .setOfferRealty(
          offer.getOfferRealty.toBuilder
            .clearPhoto()
            .addAllPhoto(
              urls.keys.map(BasicsModel.Photo.newBuilder().setExternalUrl(_).build()).asJava
            )
        )
  }
}
