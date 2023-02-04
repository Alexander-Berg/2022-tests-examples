package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.TestUtils

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DeleteOldPhotosWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new DeleteOldPhotosWorkerYdb with YdbWorkerTestImpl
  }

  ("fill deleted timestamp") in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", isDeleted = false)
    addPhoto(builder, 1, "123-name2", isDeleted = true, deleteTimeAgo = None)

    val offer = builder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)
    resultOffer.getOfferAutoru.getPhotoCount shouldBe 2

    val photoMap = resultOffer.photos.map(photo => photo.getName -> photo).toMap

    photoMap("123-name1").hasDeletedTimestamp shouldBe false
    photoMap("123-name2").getDeletedTimestamp shouldBe (System.currentTimeMillis() +- 1000)
  }

  ("nothing to delete") in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", isDeleted = false)
    (1 to 30).foreach(i => {
      addPhoto(builder, i, s"123-name${i + 1}", isDeleted = true, deleteTimeAgo = Some(2.days))
    })

    val offer = builder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)
    offer.getOfferAutoru.getPhotoCount shouldBe 31

    assert(result.updateOfferFunc.isEmpty, "nothing to delete")
    assert(result.nextCheck.isEmpty, "should not revisit")
  }

  ("delete obsolete images, keep last 30") in new Fixture {
    val builder = TestUtils.createOffer()
    addPhoto(builder, 0, "123-name1", isDeleted = false)
    (1 to 35).foreach(i => {
      addPhoto(builder, i, s"123-name${i + 1}", isDeleted = true, deleteTimeAgo = Some(i.day))
    })

    val offer = builder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None)
    result.updateOfferFunc.get(offer).getOfferAutoru.getPhotoCount shouldBe 31
    result.updateOfferFunc.get(offer).getOfferAutoru.getPhoto(0).getName shouldBe "123-name1"
    (1 to 30).foreach(i => {
      result.updateOfferFunc.get(offer).getOfferAutoru.getPhoto(i).getName shouldBe s"123-name${i + 1}"
    })

    assert(result.nextCheck.isEmpty, "should not revisit")
  }

  private def addPhoto(b: Offer.Builder,
                       num: Int,
                       name: String = "123-name",
                       isDeleted: Boolean = false,
                       deleteTimeAgo: Option[FiniteDuration] = None) = {
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

    deleteTimeAgo.foreach(time => builder.setDeletedTimestamp(System.currentTimeMillis() - time.toMillis))
    builder
  }

}
