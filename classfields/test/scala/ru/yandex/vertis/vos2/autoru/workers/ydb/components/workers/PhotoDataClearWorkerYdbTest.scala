package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.PhotoMetadata
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils

import scala.jdk.CollectionConverters._

class PhotoDataClearWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val offer: Offer

    val worker = new PhotoDataClearWorkerYdb with YdbWorkerTestImpl
  }

  "clear meta for deleted with duplicate by cv_hash" in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", hasMeta = true, cvHash = "hash1", isDeleted = false)
    addPhoto(builder, 1, "123-name2", hasMeta = true, cvHash = "hash2", isDeleted = true)
    addPhoto(builder, 2, "123-name3", hasMeta = true, cvHash = "hash3", isDeleted = false)
    addPhoto(builder, 3, "123-name4", hasMeta = true, cvHash = "hash2", isDeleted = true)

    val offer = builder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)
    val photoMap = resultOffer.getOfferAutoru.getPhotoList.asScala.map(photo => photo.getName -> photo).toMap

    assert(photoMap("123-name1").hasMeta)
    assert(!photoMap("123-name2").hasMeta)
    assert(photoMap("123-name3").hasMeta)
    assert(photoMap("123-name4").hasMeta)
  }

  "don't clear meta for deleted if not duplicate by cv_hash" in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", hasMeta = true, cvHash = "hash1", isDeleted = false)
    addPhoto(builder, 1, "123-name2", hasMeta = true, cvHash = "hash2", isDeleted = true)
    addPhoto(builder, 2, "123-name3", hasMeta = true, cvHash = "hash3", isDeleted = false)
    addPhoto(builder, 3, "123-name4", hasMeta = true, cvHash = "hash4", isDeleted = true)

    val offer = builder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)

    assert(result.updateOfferFunc.isEmpty)
  }

  "don't process offers without deleted photo" in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", hasMeta = true, isDeleted = false)
    addPhoto(builder, 1, "123-name2", hasMeta = true, isDeleted = false)

    val offer = builder.build()

    assert(!worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)

    assert(result.updateOfferFunc.isEmpty)

  }

  "don process offers with deleted whotut without meta" in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", hasMeta = false, isDeleted = true)

    val offer = builder.build()

    assert(!worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)

    assert(result.updateOfferFunc.isEmpty)

  }

  private def addPhoto(b: Offer.Builder,
                       num: Int,
                       name: String = "123-name",
                       hasMeta: Boolean = true,
                       cvHash: String = "hash1",
                       isDeleted: Boolean = false) = {
    b.getOfferAutoruBuilder.addPhotoBuilder()
    val builder = b.getOfferAutoruBuilder.getPhotoBuilder(num)
    builder
      .setIsMain(false)
      .setOrder(0)
      .setSmartOrder(0)
      .setName(name)
      .setCreated(123)
      .setOrigName(name)
      .setDeleted(isDeleted)

    if (hasMeta) builder.setMeta(createMeta(cvHash))
    builder
  }

  private def createMeta(cvHash: String): PhotoMetadata = {
    PhotoMetadata.newBuilder().setVersion(1).setIsFinished(true).setCvHash(cvHash).build()
  }
}
