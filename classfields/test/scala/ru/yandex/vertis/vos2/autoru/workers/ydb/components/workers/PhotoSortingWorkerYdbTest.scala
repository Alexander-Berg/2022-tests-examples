package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PhotoSortingWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  val featureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featureRegistry)

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new PhotoSortingWorkerYdb with YdbWorkerTestImpl {
      override def features = featuresManager
    }
  }

  val offer = OfferModel.Offer
    .newBuilder()
    .setOfferID("10454647-test")
    .setOfferService(OfferModel.OfferService.OFFER_AUTO)
    .setOfferAutoru(AutoruOffer.newBuilder().setVersion(0).setCategory(Category.CARS))
    .setTimestampUpdate(0)
    .setUserRef("user")
    .build()

  private def photo(name: String,
                    classificationToWeight: Map[Int, Int] = Map.empty,
                    leftWeight: Int = -1,
                    order: Int = 0): Photo = {
    val builder = Photo.newBuilder().setIsMain(false).setOrder(0).setName(name).setCreated(0L).setOrder(order)
    for ((classId, weight) <- classificationToWeight) {
      builder.getMetaBuilder
        .addAutoViewAngleBuilder()
        .setClassId(classId)
        .setWeight(weight)
    }
    if (leftWeight >= 0) {
      builder.getMetaBuilder.getAutoClassificationBuilder
        .setAutoSideLeftWeight(leftWeight)
    }
    if (builder.hasMeta) {
      builder.getMetaBuilder.setVersion(0).setIsFinished(true)
    }
    builder.build()
  }

  private def offerWithPhotos(photos: Photo*) = {
    val builder = offer.toBuilder
    builder.getOfferAutoruBuilder.setDisablePhotoReorder(false)
    builder.getOfferAutoruBuilder.addAllPhoto(photos.asJava)
    builder.build()
  }

  "Sort by class" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val offer = offerWithPhotos(
      photo("keys", Map(7 -> 5, 2 -> 5, 10 -> 200)),
      photo("front", Map(7 -> 200, 2 -> 5, 10 -> 5)),
      photo("fuzzy", Map(7 -> 5, 2 -> 5, 10 -> 5)),
      photo("back", Map(7 -> 5, 2 -> 200, 10 -> 5))
    )

    val photos = worker.process(offer, None).updateOfferFunc.get(offer).getOfferAutoru.getPhotoList.asScala
    photos.map(_.getName).toList shouldEqual List("front", "back", "fuzzy", "keys")
    photos.map(_.getSmartOrder).toList shouldEqual (1 to photos.size).toList
  }

  "Skip classes by threshold, preserve initial order" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val offer = offerWithPhotos(
      photo("keys", Map(7 -> 5, 2 -> 5, 10 -> 50)),
      photo("front", Map(7 -> 50, 2 -> 5, 10 -> 5)),
      photo("fuzzy", Map(7 -> 5, 2 -> 5, 10 -> 5)),
      photo("real front", Map(7 -> 200, 2 -> 5, 10 -> 5)),
      photo("back", Map(7 -> 5, 2 -> 50, 10 -> 5))
    )

    val photos = worker.process(offer, None).updateOfferFunc.get(offer).getOfferAutoru.getPhotoList.asScala
    photos.map(_.getName).toList shouldEqual List("real front", "keys", "front", "fuzzy", "back")
    photos.map(_.getSmartOrder).toList shouldEqual (1 to photos.size).toList
  }

  "Prefer left views" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val offer = offerWithPhotos(
      photo("front right", Map(7 -> 200, 2 -> 5, 10 -> 5), leftWeight = 5),
      photo("front left", Map(7 -> 200, 2 -> 5, 10 -> 5), leftWeight = 200),
      photo("back right", Map(7 -> 5, 2 -> 200, 10 -> 5), leftWeight = 10),
      photo("back less right", Map(7 -> 5, 2 -> 200, 10 -> 5), leftWeight = 11)
    )

    val photos = worker.process(offer, None).updateOfferFunc.get(offer).getOfferAutoru.getPhotoList.asScala
    photos.map(_.getName).toList shouldEqual List("front left", "front right", "back less right", "back right")
    photos.map(_.getSmartOrder).toList shouldEqual (1 to photos.size).toList
  }

  "Prefer views with biggest weight" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val offer = offerWithPhotos(
      photo("front right", Map(7 -> 200, 2 -> 5, 10 -> 5), leftWeight = 5),
      photo("front left", Map(7 -> 199, 2 -> 5, 10 -> 5), leftWeight = 200),
      photo("back right", Map(7 -> 5, 2 -> 200, 10 -> 5), leftWeight = 10),
      photo("less back right", Map(7 -> 5, 2 -> 100, 10 -> 5), leftWeight = 10)
    )

    val photos = worker.process(offer, None).updateOfferFunc.get(offer).getOfferAutoru.getPhotoList.asScala
    photos.map(_.getName).toList shouldEqual List("front right", "front left", "back right", "less back right")
    photos.map(_.getSmartOrder).toList shouldEqual (1 to photos.size).toList
  }

  "Restore original order" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, false)

    val offer = offerWithPhotos(
      photo("front right", Map(7 -> 200, 2 -> 5, 10 -> 5), leftWeight = 5, order = 5),
      photo("front left", Map(7 -> 199, 2 -> 5, 10 -> 5), leftWeight = 200, order = 1),
      photo("back right", Map(7 -> 5, 2 -> 200, 10 -> 5), leftWeight = 10, order = 7),
      photo("less back right", Map(7 -> 5, 2 -> 100, 10 -> 5), leftWeight = 10, order = 3)
    )
    val photos = worker.process(offer, None).updateOfferFunc.get(offer).getOfferAutoru.getPhotoList.asScala
    photos.map(_.getName).toList shouldEqual List("front left", "less back right", "front right", "back right")
    photos.map(_.getOrder).toList shouldEqual List(1, 3, 5, 7)
  }

  "Don't crash on incomplete meta" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val offerWoPhoto = offer
    val offerWoMeta = offerWithPhotos(photo("1"))
    val offerWoViewClassification = offerWithPhotos(photo("1", leftWeight = 10))
    val offerWoLeftClassification = offerWithPhotos(photo("1", classificationToWeight = Map(100500 -> 100500)))
    worker.process(offerWoPhoto, None)
    worker.process(offerWoMeta, None)
    worker.process(offerWoViewClassification, None)
    worker.process(offerWoLeftClassification, None)
  }

  "Correctly parse config" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    val orders = PhotoSortingWorkerYdb.parseOrderConfig("1L 2A 3R 1R 3L 4")
    def order(classId: Int, isLeft: Boolean): Double = orders(PhotoSortingWorkerYdb.toOrderKey(classId, isLeft))

    order(1, isLeft = true) should be < order(2, isLeft = true)
    order(2, isLeft = true) should be < order(1, isLeft = false)
    order(2, isLeft = true) shouldBe order(2, isLeft = false)
    order(2, isLeft = true) should be < order(3, isLeft = true)
    order(3, isLeft = false) should be < order(3, isLeft = true)
    order(2, isLeft = true) should be < order(4, isLeft = true)
    order(4, isLeft = true) should be < order(100000, isLeft = true)
  }

  "Actual config should include fuzzy class" in new Fixture {
    featureRegistry.updateFeature(featuresManager.PhotoSorting.name, true)

    (PhotoSortingWorkerYdb.ClassificationOrder should contain).key("-1L")
    (PhotoSortingWorkerYdb.ClassificationOrder should contain).key("-1R")
    PhotoSortingWorkerYdb.ClassificationOrder("-1L") should be < PhotoSortingWorkerYdb.ClassificationOrder(
      "undefined key"
    )
  }
}
