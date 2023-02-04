package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import java.io.File

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo.RecognizedNumber
import ru.yandex.vos2.BasicsModel.{PhotoTransform, PhotoTransformHistory}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.BlurCoordinates
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.services.blur.{FileBlurResult, HttpLicensePlateBlur}
import ru.yandex.vos2.autoru.utils.PhotoUtilsGenerator

class LicensePlateBlurWorkerYdbTest
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with InitTestDbs
  with BeforeAndAfterAll {

  implicit val traced: Traced = Traced.empty

  private val TestOfferID = "123-abc"

  private val TestLicensePlate = RecognizedNumber
    .newBuilder()
    .setNumber("A777AA77")
    .setConfidence(1)
    .setWidthPercent(1)
    .build()

  initDbs()

  abstract private class Fixture {
    val offer: Offer

    val worker = new LicensePlateBlurWorkerYdb(
      components.photoUtils,
      components.mdsPhotoUtils
    ) with YdbWorkerTestImpl
  }

  "should not process offer without photo" in new Fixture {
    val offerBuilder = createOffer()
    val offer = offerBuilder.build()
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "should not process offer with only blurred or deleted photo or not found in mds photo" in new Fixture {
    val offerBuilder = createOffer()

    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = true, isDeleted = false, notFoundInMds = false)
    addPhoto2(offerBuilder, 1, haveNumber = true, isBlurred = false, isDeleted = true, notFoundInMds = false)
    addPhoto2(offerBuilder, 2, haveNumber = true, isBlurred = false, isDeleted = false, notFoundInMds = true)
    offerBuilder.getOfferAutoruBuilder.setNeedBlurPhoto(true)
    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)

  }

  "should not process offer: all photo already blurred" in new Fixture {
    val offerBuilder = createOffer()
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = true)
    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "photo not blurred, needBlur = true" in new Fixture {
    val offerBuilder = createOffer()
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = false)

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "dealer, hide_license_plate = false" in new Fixture {
    val offerBuilder = createOffer(dealer = true)
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = false, removeBlur = None)

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }
  "dealer, hide_license_plate = false, photo is blurred" in new Fixture {
    val offerBuilder = createOffer(dealer = true)
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = true, removeBlur = None)

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }
  "dealer, hide_license_plate = true" in new Fixture {
    val offerBuilder = createOffer(dealer = true)
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = false)
    offerBuilder.getOfferAutoruBuilder.getSalonBuilder.setHideLicensePlate(true)

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "private, inactive offer" in new Fixture {
    val offerBuilder = createOffer()
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = false)
    offerBuilder.addFlag(OfferFlag.OF_INACTIVE)

    val offer = offerBuilder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "unblur blurred offer" in new Fixture {
    val offerBuilder = createOffer()
    addPhoto2(offerBuilder, 0, haveNumber = true, isBlurred = true, removeBlur = Some(true))

    val offer = offerBuilder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  private lazy val licensePlateBlur = new HttpLicensePlateBlur("") {

    override def blur(file: File, blurCoordinates: Option[BlurCoordinates])(implicit trace: Traced): FileBlurResult = {
      FileBlurResult(file, Seq(TestLicensePlate))
    }
  }

  val photoUtils = PhotoUtilsGenerator.generatePhotoUtils

  private def addPhoto(b: Offer.Builder,
                       num: Int,
                       isBlurred: Boolean = false,
                       addTransformHistory: Boolean = true,
                       angle: Int = 0,
                       name: String) = {
    b.getOfferAutoruBuilder.addPhotoBuilder()
    val builder = b.getOfferAutoruBuilder.getPhotoBuilder(num)
    builder
      .setIsMain(false)
      .setOrder(0)
      .setSmartOrder(0)
      .setName(name)
      .setCreated(123)
      .setOrigName(name)

    if (addTransformHistory) {
      builder.addTransformHistory(
        PhotoTransformHistory
          .newBuilder()
          .setName(name)
          .setTransform(PhotoTransform.newBuilder().setBlur(isBlurred).setAngle(angle))
      )
      builder.setCurrentTransform(PhotoTransform.newBuilder().setBlur(isBlurred).setAngle(angle))
    }
  }

  private def addPhoto2(b: Offer.Builder,
                        num: Int,
                        haveNumber: Boolean,
                        isBlurred: Boolean = false,
                        removeBlur: Option[Boolean] = Some(false),
                        addTransformHistory: Boolean = true,
                        angle: Int = 0,
                        name: String = "123-name",
                        isDeleted: Boolean = false,
                        notFoundInMds: Boolean = false) = {
    b.getOfferAutoruBuilder.addPhotoBuilder()
    val builder = b.getOfferAutoruBuilder.getPhotoBuilder(num)
    builder
      .setIsMain(false)
      .setOrder(0)
      .setSmartOrder(0)
      .setName(name)
      .setNamespace("autoru-all")
      .setOrigName(name)
      .setOrigNamespace("autoru-all")
      .setCreated(123)
      .setDeleted(isDeleted)
      .setNotFoundInMds(notFoundInMds)

    removeBlur.foreach(builder.setRemoveBlur)

    if (notFoundInMds) {
      builder
        .addPhotoCheckExistCacheBuilder()
        .setName(name)
        .setNamespace("autoru-all")
        .setNotFound(true)
    }

    if (haveNumber) {
      builder.addNumbers(number)
    }
    if (addTransformHistory) {
      builder.addTransformHistory(
        PhotoTransformHistory
          .newBuilder()
          .setName("123-source-name")
          .setTransform(PhotoTransform.newBuilder().setBlur(isBlurred).setAngle(angle))
      )
      builder.setCurrentTransform(PhotoTransform.newBuilder().setBlur(isBlurred).setAngle(angle))
    }
  }

  private val number = RecognizedNumber
    .newBuilder()
    .setNumber("A777AA77")
    .setConfidence(1)
    .setWidthPercent(1)
    .build()
}
