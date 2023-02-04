package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.BasicsModel.Photo.CheckExistResult
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.getNow
import ru.yandex.vos2.services.mds.MdsPhotoData

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.DurationInt

class PhotoCheckExistWorkerYdbTest
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

    val worker = new PhotoCheckExistWorkerYdb(
      mdsUploader
    ) with YdbWorkerTestImpl
  }

  "PhotoCheck Worker YDB" should {

    "current & orig not found" in new Fixture {
      val current = MdsPhotoData("autoru-all", "123-abc")
      val orig = MdsPhotoData("autoru-all", "456-def")
      val offer = createOffer(Seq(PhotoInfo(current, orig))).build()
      components.offerVosDao.saveMigrated(Seq(offer), "test")(Traced.empty)
      when(mdsUploader.checkOrigPhotoExist(eqq(orig))(?))
        .thenReturn(false)
      when(mdsUploader.checkOrigPhotoExist(eqq(current))(?))
        .thenReturn(false)
      val res = worker.process(offer, None)
      val updatedOffer = res.updateOfferFunc.get(offer)
      val notFound = updatedOffer.getOfferAutoru.getPhoto(0).getPhotoCheckExistCacheList.asScala
      assert(notFound.size == 2)
      assert(notFound.forall(_.getNotFound))
    }

    "current not found" in new Fixture {
      val current = MdsPhotoData("autoru-all", "123-abc")
      val orig = MdsPhotoData("autoru-all", "456-def")

      val offer = createOffer(Seq(PhotoInfo(current, orig))).build()
      components.offerVosDao.saveMigrated(Seq(offer), "test")(Traced.empty)
      when(mdsUploader.checkOrigPhotoExist(eqq(orig))(?))
        .thenReturn(true)
      when(mdsUploader.checkOrigPhotoExist(eqq(current))(?))
        .thenReturn(false)
      val res = worker.process(offer, None)
      val updatedOffer = res.updateOfferFunc.get(offer)
      val notFound = updatedOffer.getOfferAutoru.getPhoto(0).getPhotoCheckExistCacheList.asScala
      assert(notFound.size === 2)
      assert(notFound.count(_.getNotFound) == 1)
      assert(notFound.find(_.getNotFound).get.getName == current.name)
    }

    "one photo with not found orig" in new Fixture {
      val current1 = MdsPhotoData("autoru-vos", "1-vos")
      val orig1 = MdsPhotoData("autoru-orig", "1-orig")
      val current2 = MdsPhotoData("autoru-vos", "2-vos")
      val orig2 = MdsPhotoData("autoru-orig", "2-orig")

      val offer = createOffer(Seq(PhotoInfo(current1, orig1), PhotoInfo(current2, orig2))).build()
      components.offerVosDao.saveMigrated(Seq(offer), "test")(Traced.empty)

      when(mdsUploader.checkOrigPhotoExist(eqq(orig1))(?))
        .thenReturn(true)
      when(mdsUploader.checkOrigPhotoExist(eqq(current1))(?))
        .thenReturn(true)
      when(mdsUploader.checkOrigPhotoExist(eqq(orig2))(?))
        .thenReturn(false)
      when(mdsUploader.checkOrigPhotoExist(eqq(current2))(?))
        .thenReturn(true)

      val res = worker.process(offer, None)
      val updatedOffer = res.updateOfferFunc.get(offer)

      val notFound1 = updatedOffer.getOfferAutoru.getPhoto(0).getPhotoCheckExistCacheList.asScala
      val notFound2 = updatedOffer.getOfferAutoru.getPhoto(1).getPhotoCheckExistCacheList.asScala

      assert(notFound1.size == 2)
      assert(notFound1.forall(!_.getNotFound))

      assert(notFound2.size == 2)
      assert(notFound2.count(_.getNotFound) == 1)
      assert(notFound2.find(_.getNotFound).get.getName == orig2.name)
    }

    "recently checked" in new Fixture {
      val current = MdsPhotoData("autoru-all", "123-abc")
      val orig = MdsPhotoData("autoru-all", "456-def")

      val builder = createOffer(Seq(PhotoInfo(current, orig)))
      builder.getOfferAutoruBuilder.getPhotoBuilder(0).setLastCheckExistsTimestamp(getNow)

      val offer = builder.build()
      components.offerVosDao.saveMigrated(Seq(offer), "test")(Traced.empty)

      when(mdsUploader.checkOrigPhotoExist(eqq(orig))(?))
        .thenReturn(false)
      when(mdsUploader.checkOrigPhotoExist(eqq(current))(?))
        .thenReturn(false)

      val res = worker.process(offer, None)

      assert(
        res.updateOfferFunc.isEmpty
      )
    }

  }

  "current & orig not found" in new Fixture {
    val current = MdsPhotoData("autoru-all", "123-abc")
    val orig = MdsPhotoData("autoru-all", "456-def")

    val offer = createOffer(Seq(PhotoInfo(current, orig))).build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "recently checked" in new Fixture {
    val current = MdsPhotoData("autoru-all", "123-abc")
    val orig = MdsPhotoData("autoru-all", "456-def")

    val builder = createOffer(Seq(PhotoInfo(current, orig)))
    builder.getOfferAutoruBuilder.getPhotoBuilder(0).setLastCheckExistsTimestamp(getNow)
    val offer = builder.build()

    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  case class PhotoInfo(id: MdsPhotoData, origId: MdsPhotoData, notFound: Boolean = false, origNotFound: Boolean = false)

  private def createOffer(photoInfo: Seq[PhotoInfo]): Offer.Builder = {
    val builder = TestUtils.createOffer()
    builder.setOfferID(TestOfferID)

    val photo = photoInfo.map(info => {
      val builder = Photo.newBuilder()
      builder
        .setIsMain(false)
        .setOrder(1)
        .setCreated(1)
        .setName(info.id.name)
        .setNamespace(info.id.namespace)
        .setOrigName(info.origId.name)
        .setOrigNamespace(info.origId.namespace)
        .setLastCheckExistsTimestamp(getNow - 9.days.toMillis)
      builder.addPhotoCheckExistCache(createCheckExistResult(info.id, info.notFound))
      builder.addPhotoCheckExistCache(createCheckExistResult(info.origId, info.origNotFound))
    })

    photo.foreach(builder.getOfferAutoruBuilder.addPhoto)

    builder
  }
  private val TestOfferID = "123-checkexist"

  private def createCheckExistResult(photoId: MdsPhotoData, notFound: Boolean): CheckExistResult = {
    CheckExistResult
      .newBuilder()
      .setName(photoId.name)
      .setNamespace(photoId.namespace)
      .setNotFound(notFound)
      .build()
  }

}
