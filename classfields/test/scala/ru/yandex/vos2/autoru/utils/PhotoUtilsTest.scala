package ru.yandex.vos2.autoru.utils

import java.io.{File, InputStream}
import javax.imageio.ImageIO
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo.CheckExistResult
import ru.yandex.vos2.BasicsModel.{Photo, PhotoOrBuilder, PhotoTransform, PhotoTransformHistory}
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName.AUTORU
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.autoru.utils.PhotoUtilsGenerator.{imageGenerator, replaceNamePrefix, urlDownloaderThrowError}
import ru.yandex.vos2.services.mds.{AutoruVosNamespaceSettings, MdsPhotoData}
import ru.yandex.vos2.util.IO

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 3/17/17.
  */
@RunWith(classOf[JUnitRunner])
class PhotoUtilsTest extends AnyFunSuite with OptionValues {
  private val photoUtils = PhotoUtilsGenerator.generatePhotoUtils
  private val imageHashGenerator = PhotoUtilsGenerator.imageHashGenerator

  implicit private val t = Traced.empty
  val namespace = "autoru-vos"
  val origNamespace = "autoru-orig"

  test("uploadPhoto") {
    val imageHash = imageHashGenerator.nextValue
    val res = photoUtils.uploadPhotoFromArray(Array[Byte](1, 2, 3, 4, 5), namespace)
    assert(res == MdsPhotoData(namespace, imageHash))
  }

  test("uploadPhotoFromUrl") {
    val imageHash = imageHashGenerator.nextValue
    val res = photoUtils.uploadPhotoFromUrl("http://yandex.ru", namespace, Offer.getDefaultInstance)
    assert(res == SuccessfullyUploadFromUrl(MdsPhotoData(namespace, imageHash)))
  }

  test("uploadPhotoFromUrl second time from the same url") {
    val offerBuilder = TestUtils.createOffer()
    val SuccessfullyUploadFromUrl(imageHash) =
      photoUtils.uploadPhotoFromUrl("http://yandex.ru", namespace, offerBuilder)
    val photo = PhotoUtils.addPhoto(offerBuilder, imageHash, Some("http://yandex.ru"))

    val res = photoUtils.uploadPhotoFromUrl("http://yandex.ru", namespace, offerBuilder)
    assert(res == PhotoAlreadyExists(photo))
  }

  test("addPhoto with same external Url") {
    val offerBuilder = TestUtils.createOffer()
    val externalUrl = "http://yandex.ru"
    val testHash = MdsPhotoData(namespace, "hash-2")
    val SuccessfullyUploadFromUrl(imageHash) =
      photoUtils.uploadPhotoFromUrl(externalUrl, namespace, offerBuilder)
    PhotoUtils.addPhoto(offerBuilder, imageHash, Option(externalUrl))
    PhotoUtils.addPhoto(offerBuilder, testHash, Option(externalUrl))
    assert(offerBuilder.getOfferAutoru.getPhotoCount == 1)
  }

  test("addPhoto") {
    val offerBuilder = TestUtils.createOffer()
    val imageHash = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, imageHash, None)
    checkOffer(offerBuilder, 1, imageHash, imageHash, (0, false), (imageHash, 0, false))
  }

  test("addPhoto from external Url") {
    val offerBuilder = TestUtils.createOffer()
    val imageHash = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, imageHash, Some("http://yandex.ru"))
    checkOffer(offerBuilder, 1, imageHash, imageHash, (0, false), (imageHash, 0, false))
    val photo: Photo = offerBuilder.getOfferAutoru.getPhoto(0)
    assert(photo.getExternalUrl == "http://yandex.ru")
  }

  test("update external url in photo") {
    val offerBuilder = TestUtils.createOffer()
    val imageHash = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, imageHash, None)
    PhotoUtils.addPhoto(offerBuilder, imageHash, Some("http://yandex.ru"))
    checkOffer(offerBuilder, 1, imageHash, imageHash, (0, false), (imageHash, 0, false))
    val photo: Photo = offerBuilder.getOfferAutoru.getPhoto(0)
    assert(photo.getExternalUrl == "http://yandex.ru")
  }

  test("rotatePhoto") {
    val offerBuilder = TestUtils.createOffer()

    // добавляем фото
    val orig = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, orig, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilder(0)

    // поворачиваем фото на 90 градусов - создается одна новая трансформация (всего две)
    val rotated90 = MdsPhotoData(namespace, nextHashForPrefix(orig))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(offerBuilder, 1, rotated90, orig, (90, false), (orig, 0, false), (rotated90, 90, false))

    // поворачиваем фото еще на 90 градусов - создается одна новая трансформация (всего три)
    val rotated180 = MdsPhotoData(namespace, nextHashForPrefix(rotated90))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(
      offerBuilder,
      1,
      rotated180,
      orig,
      (180, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (rotated180, 180, false)
    )

    // поворачиваем фото еще на 90 градусов - создается одна новая трансформация (всего четыре)
    val rotated270 = MdsPhotoData(namespace, nextHashForPrefix(rotated180))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(
      offerBuilder,
      1,
      rotated270,
      orig,
      (270, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (rotated180, 180, false),
      (rotated270, 270, false)
    )

    // прошли полный круг, новая трансформация не создается
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(
      offerBuilder,
      1,
      orig,
      orig,
      (0, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (rotated180, 180, false),
      (rotated270, 270, false)
    )

    // вертим обратно, новая трансформация не создается
    photoUtils.rotatePhoto(photoBuilder, cw = false)
    checkOffer(
      offerBuilder,
      1,
      rotated270,
      orig,
      (270, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (rotated180, 180, false),
      (rotated270, 270, false)
    )
  }

  test("blurPhoto") {
    val offerBuilder = TestUtils.createOffer()

    val orig = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, orig, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilder(0)

    val rotated90 = MdsPhotoData(namespace, nextHashForPrefix(orig))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(offerBuilder, 1, rotated90, orig, (90, false), (orig, 0, false), (rotated90, 90, false))

    // замазываем повернутое фото. будет также создана трансформация - замазывание оригинала,
    // потому что его еще не было. Поэтому всего четыре трансформации
    val blurOrig = MdsPhotoData("autoru-vos", nextHashForPrefix(rotated90))
    val blurRotated90 = MdsPhotoData("autoru-vos", nextHashForPrefix(blurOrig))
    photoUtils.blurPhoto(photoBuilder, needBlur = true, blurCoordinates = None)
    checkOffer(
      offerBuilder,
      1,
      blurRotated90,
      orig,
      (90, true),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true)
    )

    photoUtils.blurPhoto(photoBuilder, needBlur = false, blurCoordinates = None)
    checkOffer(
      offerBuilder,
      1,
      rotated90,
      orig,
      (90, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true)
    )

    val blurRotated180 = MdsPhotoData("autoru-vos", nextHashForPrefix(blurRotated90))
    photoUtils.blurPhoto(photoBuilder, needBlur = true, blurCoordinates = None)
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    checkOffer(
      offerBuilder,
      1,
      blurRotated180,
      orig,
      (180, true),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true),
      (blurRotated180, 180, true)
    )

    // убедимся, что от нас не осталось файлов в /tmp
    val tmpDir = System.getProperty("java.io.tmpdir")
    assert(
      new File(tmpDir)
        .listFiles()
        .forall(file => {
          val name: String = file.getName
          !(file.isFile && name.startsWith("autoru") && (name.endsWith("_photo") || name.endsWith("_photo_blur")))
        })
    )
  }

  test("change namespace") {
    val offerBuilder = TestUtils.createOffer()

    val orig = MdsPhotoData("autoru-orig", imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, orig, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilder(0)

    val newPhoto = MdsPhotoData("autoru-vos", nextHashForPrefix(orig))
    photoUtils.changeNamespace(photoBuilder, "autoru-vos")

    checkOffer(offerBuilder, 1, newPhoto, orig, (0, false), (orig, 0, false), (newPhoto, 0, false))
  }

  test("updatePhoto") {
    val offerBuilder = TestUtils.createOffer()

    val orig = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, orig, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilder(0)

    val rotated90 = MdsPhotoData(namespace, nextHashForPrefix(orig))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    val blurOrig = MdsPhotoData("autoru-vos", nextHashForPrefix(rotated90))
    val blurRotated90 = MdsPhotoData("autoru-vos", nextHashForPrefix(blurOrig))
    photoUtils.blurPhoto(photoBuilder, needBlur = true, blurCoordinates = None)

    val photoBuilder2 = Photo.newBuilder()
    photoUtils.updatePhoto(photoBuilder, photoBuilder2)

    checkPhoto(
      photoBuilder2,
      blurRotated90,
      orig,
      (90, true),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true)
    )

    // мы не проверяем и не гарантируем уникальность состояний в истории, а храним все
    val photoBuilder3 = photoBuilder.clone()
    photoBuilder3.getTransformHistoryBuilderList.asScala.foreach(b => b.setName(b.getName + "3"))
    photoUtils.updatePhoto(photoBuilder, photoBuilder3)

    checkPhoto(
      photoBuilder3,
      blurRotated90,
      orig,
      (90, true),
      (MdsPhotoData(orig.namespace, orig.name + "3"), 0, false),
      (MdsPhotoData(rotated90.namespace, rotated90.name + "3"), 90, false),
      (MdsPhotoData(blurOrig.namespace, blurOrig.name + "3"), 0, true),
      (MdsPhotoData(blurRotated90.namespace, blurRotated90.name + "3"), 90, true),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true)
    )
  }

  test("restoreOrigPhoto") {
    val offerBuilder = TestUtils.createOffer()

    val orig = MdsPhotoData(origNamespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, orig, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilder(0)

    val rotated90 = MdsPhotoData(origNamespace, nextHashForPrefix(orig))
    photoUtils.rotatePhoto(photoBuilder, cw = true)
    val blurOrig = MdsPhotoData("autoru-vos", nextHashForPrefix(rotated90))
    val blurRotated90 = MdsPhotoData("autoru-vos", nextHashForPrefix(blurOrig))
    photoUtils.blurPhoto(photoBuilder, needBlur = true, blurCoordinates = None)

    val restoredOrig = MdsPhotoData(AutoruVosNamespaceSettings.namespace, nextHashForPrefix(blurRotated90))
    photoUtils.restoreOrigPhoto(photoBuilder)
    checkPhoto(
      photoBuilder,
      restoredOrig,
      orig,
      (0, false),
      (orig, 0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true),
      (restoredOrig, 0, false)
    )

    // если в истории нет оригинала
    photoBuilder.removeTransformHistory(0)
    photoUtils.restoreOrigPhoto(photoBuilder)
    checkPhoto(
      photoBuilder,
      restoredOrig,
      orig,
      (0, false),
      (rotated90, 90, false),
      (blurOrig, 0, true),
      (blurRotated90, 90, true),
      (restoredOrig, 0, false)
    )
  }

  test("findPhoto") {
    val offerBuilder = TestUtils.createOffer()

    val one = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    val two = MdsPhotoData(namespace, imageHashGenerator.generateValue)
    PhotoUtils.addPhoto(offerBuilder, one, None)
    PhotoUtils.addPhoto(offerBuilder, two, None)

    assert(PhotoUtils.findPhotoById(offerBuilder, classified = AUTORU, one).nonEmpty)
    assert(PhotoUtils.findPhotoById(offerBuilder, classified = AUTORU, MdsPhotoData(namespace, "xxx-yyy")).isEmpty)

    val photo2: (Photo.Builder, Int) = PhotoUtils.findPhotoWithIdx(offerBuilder, two).value
    assert(photo2._1.getId == two)
    assert(photo2._2 == 1)
  }

  test("transform history with equals photo name") {
    val offerBuilder = TestUtils.createOffer()
    val photoName = MdsPhotoData(namespace, imageHashGenerator.generateValue)

    val mdsUploader = new MdsUploader {
      override def putFromInputStream(in: InputStream,
                                      namespace: String,
                                      prefix: Option[String])(implicit trace: Traced): MdsPhotoData =
        MdsPhotoData(namespace, "abc-xyz")

      override def putFromArray(photo: Array[Byte], namespace: String)(implicit trace: Traced): MdsPhotoData =
        MdsPhotoData(namespace, "abc-xyz")

      override def putFromUrl(url: String, namespace: String)(implicit trace: Traced): MdsPhotoData = {
        if (!urlDownloaderThrowError.get()) {
          MdsPhotoData(namespace, "abc-xyz")
        } else {
          sys.error("urlDownloader error")
        }
      }

      private def generateFile(): File = {
        val image = imageGenerator.generateValue
        val tmpFile = IO.newTempFile("autoru", "_photo")
        ImageIO.write(image, "jpeg", tmpFile)
        tmpFile
      }

      override def actionWithOrigPhoto[T](
          photoID: MdsPhotoData,
          checkExistCache: Seq[CheckExistResult] = Seq.empty
      )(action: File => T)(implicit trace: Traced): T = {
        val file = generateFile()
        try {
          action(file)
        } finally {
          file.delete()
        }
      }

      override def actionWithPhotoBySize[T](
          photoID: MdsPhotoData,
          size: String,
          checkExistCache: Seq[CheckExistResult] = Seq.empty
      )(action: File => T)(implicit trace: Traced): T = {
        val file = generateFile()
        try {
          action(file)
        } finally {
          file.delete()
        }
      }

      override def remove(photoId: MdsPhotoData)(implicit trace: Traced): Int = 200

      override def checkOrigPhotoExist(photoId: MdsPhotoData)(implicit trace: Traced): Boolean = true
    }

    val licensePlateBlur = PhotoUtilsGenerator.generateLicensePlateBlur
    val ocrClient = PhotoUtilsGenerator.generateOcrClient
    val photoUtils = new PhotoUtils(mdsUploader, licensePlateBlur, ocrClient)

    PhotoUtils.addPhoto(offerBuilder, photoName, None)
    val photoBuilder = offerBuilder.getOfferAutoruBuilder.getPhotoBuilderList.asScala
      .filter(_.getId == photoName)
      .head
    photoBuilder.clearTransformHistory()

    val transform = PhotoTransform.newBuilder().setAngle(0).setBlur(false)
    photoBuilder.setCurrentTransform(transform)
    photoBuilder.addTransformHistory(
      PhotoTransformHistory
        .newBuilder()
        .setName("abcd-wxyz")
        .setTransform(PhotoTransform.newBuilder().setAngle(45).setBlur(false))
    )
    photoBuilder.addTransformHistory(
      PhotoTransformHistory
        .newBuilder()
        .setName("abc-xyz")
        .setTransform(transform)
    )

    photoUtils.blurPhoto(photoBuilder, needBlur = true, blurCoordinates = None)

    assert(photoBuilder.getTransformHistoryCount == 3)
    assert(photoBuilder.getTransformHistory(0).getName == "abcd-wxyz")
    assert(!photoBuilder.getTransformHistory(0).getTransform.getBlur)
    assert(photoBuilder.getTransformHistory(0).getTransform.getAngle == 45)
    assert(photoBuilder.getTransformHistory(1).getName == "abc-xyz")
    assert(!photoBuilder.getTransformHistory(1).getTransform.getBlur)
    assert(photoBuilder.getTransformHistory(1).getTransform.getAngle == 0)
    assert(photoBuilder.getTransformHistory(2).getName == "abc-xyz")
    assert(photoBuilder.getTransformHistory(2).getTransform.getBlur)
    assert(photoBuilder.getTransformHistory(2).getTransform.getAngle == 0)
  }

  private def checkOffer(offerBuilder: Offer.Builder,
                         photoCount: Int,
                         name: MdsPhotoData,
                         origName: MdsPhotoData,
                         curTransform: (Int, Boolean),
                         transforms: (MdsPhotoData, Int, Boolean)*): Unit = {
    val offer = offerBuilder.build()
    assert(offer.getOfferAutoru.getPhotoCount == 1)
    val photo: Photo = offer.getOfferAutoru.getPhoto(0)
    checkPhoto(photo, name, origName, curTransform, transforms: _*)
  }

  def checkPhoto(photo: PhotoOrBuilder,
                 name: MdsPhotoData,
                 origName: MdsPhotoData,
                 curTransform: (Int, Boolean),
                 transforms: (MdsPhotoData, Int, Boolean)*): Unit = {
    assert(photo.getId == name, "name is wrong")
    assert(photo.getOrigId == origName, "origName is wrong")
    assert(photo.getCurrentTransform.getAngle == curTransform._1, "current angle is wrong")
    assert(photo.getCurrentTransform.getBlur == curTransform._2, "current blur is wrong")
    assert(photo.getTransformHistoryCount == transforms.length, "transform length is wrong")
    transforms.zipWithIndex.foreach {
      case ((tName, tAngle, tBlur), idx) =>
        assert(photo.getTransformHistory(idx).getId == tName, s"$idx transform name is wrong")
        assert(photo.getTransformHistory(idx).getTransform.getAngle == tAngle, s"$idx transform angle is wrong")
        assert(photo.getTransformHistory(idx).getTransform.getBlur == tBlur, s"$idx transform blur is wrong")
    }
  }

  private def nextHashForPrefix(oldPhotoId: MdsPhotoData): String = {
    replaceNamePrefix(imageHashGenerator.nextValue, oldPhotoId.prefix)
  }
}
