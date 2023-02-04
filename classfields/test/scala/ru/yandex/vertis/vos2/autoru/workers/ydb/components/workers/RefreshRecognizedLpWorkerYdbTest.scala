package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.utils._
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.BasicsModel.Photo.RecognizedNumber
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.jdk.CollectionConverters._

class RefreshRecognizedLpWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport with InitTestDbs {

  implicit val traced: Traced = Traced.empty

  implicit val ordering: Ordering[RecognizedNumber] = {
    Ordering.by(x => (x.getNumber, x.getWidthPercent, x.getConfidence))
  }

  val feature = components.featuresManager.RecognizedLicensePlateYdb
  components.featureRegistry.updateFeature(feature.name, true)

  abstract private class Fixture {
    val offer: Offer

    val worker = new RefreshRecognizedLpWorkerYdb(
      components.recognizedLpUtils
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = components.featuresManager
    }
  }

  private val TestLicensePlate = RecognizedNumber
    .newBuilder()
    .setNumber("A777AA77")
    .setConfidence(1)
    .setWidthPercent(1)
    .build()

  "recognize moderation photos" in new Fixture {

    val lp = "AA123A45"

    val moderationPhoto =
      Photo
        .newBuilder()
        .setName("test:test")
        .setIsMain(true)
        .setOrder(1)
        .setCreated(System.currentTimeMillis())
        .addNumbers(createRecognizedLp("AA123A45"))

    val offerBuilder = TestUtils.createOffer(System.currentTimeMillis())
    val autoruOfferBuilder = offerBuilder.getOfferAutoruBuilder
    autoruOfferBuilder.addModerationPhoto(moderationPhoto)

    val offer = offerBuilder.build()
    val res = worker.shouldProcess(offer, None)
    assert(res.shouldProcess)

    val prevDateTime = new DateTime().minusDays(1)
    val processed = worker.process(offer, Some(getStateStr(NextCheckData((prevDateTime)))))
    val processedOffer = processed.updateOfferFunc.get(offer)
    val photos = offer.getOfferAutoru.getPhotoList.asScala.flatMap(_.getNumbersList.asScala)
    val moderationPhotos = offer.getOfferAutoru.getModerationPhotoList.asScala.flatMap(_.getNumbersList.asScala)
    val recognizedLicensePlates = photos ++ moderationPhotos
    val sourceHash = DigestUtils.sha1Hex(recognizedLicensePlates.sorted.map(_.toString).mkString(";")).take(16)

    assert(processedOffer.getOfferAutoru.getRecognizedLp.getLicensePlate == lp)
    assert(processedOffer.getOfferAutoru.getRecognizedLp.getSourceHash == sourceHash)

  }

  private def createRecognizedLp(lp: String = ""): RecognizedNumber =
    RecognizedNumber
      .newBuilder()
      .setNumber(lp)
      .setConfidence(.8)
      .setWidthPercent(.1)
      .build()

  private val TestOfferID = "123-abc"
}
