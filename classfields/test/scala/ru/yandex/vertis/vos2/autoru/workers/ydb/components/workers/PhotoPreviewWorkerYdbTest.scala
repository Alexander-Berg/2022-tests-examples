package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.CommonModel.SmallPhotoPreview
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow

class PhotoPreviewWorkerYdbTest
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with InitTestDbs
  with BeforeAndAfter {

  initDbs()

  val mdsUploader: MdsUploader = mock[MdsUploader]

  implicit val traced = Traced.empty

  before {
    reset(mdsUploader)
    components.featureRegistry.updateFeature(components.featuresManager.PhotoCheckExistYdb.name, true)
  }

  after {
    components.featureRegistry.updateFeature(components.featuresManager.PhotoCheckExistYdb.name, false)
  }

  abstract private class Fixture {
    val offer: Offer
    val mockedFeatureManager = mock[FeaturesManager]
    val daoMocked = mock[AutoruOfferDao]
    val ydbMocked = mock[YdbWrapper]

    val worker = new PhotoPreviewWorkerYdb(
      mdsUploader,
      components.mdsPhotoUtils
    ) with YdbWorkerTestImpl

    def createOffer(now: Long = getNow, dealer: Boolean = false, regionId: Long = 1): Offer.Builder = {

      val offerBuilder = TestUtils.createOffer(now, dealer)

      offerBuilder
    }
  }

  "PhotoPreview Worker YDB" should {

    "there are photo without preview" in new Fixture {
      val offerBuilder: Offer.Builder = createOffer()
      offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearUserName()
      addPhoto(offerBuilder, 0, preview = false, "autoru-vos", "123-abc")
      val offer: Offer = offerBuilder.build()

      assert(worker.shouldProcess(offer, None).shouldProcess)
    }
    "should not be added to queue" when {
      "all photo have preview" in new Fixture {
        val offerBuilder: Offer.Builder = createOffer()
        offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearUserName()
        addPhoto(offerBuilder, 0, preview = true, "autoru-vos", "123-abc")
        addPhoto(offerBuilder, 1, preview = true, "autoru-all", "456-def")
        val offer: Offer = offerBuilder.build()

        worker.shouldProcess(offer, None).shouldProcess shouldEqual false
      }

      "photos in autoru-orig namespace" in new Fixture {
        val offerBuilder: Offer.Builder = createOffer()
        offerBuilder.getOfferAutoruBuilder.getSellerBuilder.clearUserName()
        addPhoto(offerBuilder, 0, preview = true, "autoru-orig", "123-abc")
        addPhoto(offerBuilder, 1, preview = true, "autoru-orig", "456-def")
        val offer: Offer = offerBuilder.build()

        worker.shouldProcess(offer, None).shouldProcess shouldEqual false
      }

    }
  }

  private def addPhoto(b: Offer.Builder, num: Int, preview: Boolean, namespace: String, name: String) = {
    b.getOfferAutoruBuilder.addPhotoBuilder()
    val builder = b.getOfferAutoruBuilder.getPhotoBuilder(num)
    builder
      .setIsMain(false)
      .setOrder(0)
      .setSmartOrder(0)
      .setName(name)
      .setCreated(123)
      .setOrigName(name)

    if (preview) {
      builder.setPhotoPreview(SmallPhotoPreview.newBuilder().setVersion(1))
    }
  }
}
