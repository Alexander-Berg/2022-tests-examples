package ru.yandex.vos2.autoru.utils

import java.awt.image.BufferedImage
import java.io.{File, InputStream}
import java.util.concurrent.atomic.AtomicBoolean

import javax.imageio.ImageIO
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo.{CheckExistResult, RecognizedNumber}
import ru.yandex.vos2.autoru.model.BlurCoordinates
import ru.yandex.vos2.autoru.services.blur.{FileBlurResult, FileRecognizedNumberResult, LicensePlateBlur}
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.services.mds.MdsPhotoData
import ru.yandex.vos2.services.ocr.{OcrClient, StsData}
import ru.yandex.vos2.util.{IO, RandomUtil}

class ImageHashGenerator extends AheadAwareRandomGenerator[String] {

  override protected def genValue: String =
    RandomUtil.nextInt(1000, 9000) + "-" + RandomUtil.nextHexString(6) + "_" + RandomUtil.nextHexString(32)
}

class VinGenerator extends AheadAwareRandomGenerator[String] {

  override protected def genValue: String = {
    val part1 = RandomUtil.randomSymbols(13, ('A', 'H'), ('J', 'N'), ('P', 'P'), ('R', 'Z'), ('0', '9'))
    val part2 = RandomUtil.nextDigits(4)
    s"$part1$part2"
  }
}

class ImageGenerator extends AheadAwareRandomGenerator[BufferedImage] {

  override protected def genValue: BufferedImage = {
    val width = 10
    val height = 10
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for {
      x <- 0 until width
      y <- 0 until height
    } {
      img.setRGB(x, y, RandomUtil.nextInt(256 * 256 * 256))
    }
    img
  }
}

/**
  * Created by andrey on 3/17/17.
  */
object PhotoUtilsGenerator {
  val imageHashGenerator = new ImageHashGenerator
  val vinGenerator = new VinGenerator
  val imageGenerator = new ImageGenerator

  val urlDownloaderThrowError: AtomicBoolean = new AtomicBoolean(false)

  //scalastyle:off method.length
  def generatePhotoUtils: PhotoUtils = {
    val mdsUploader = new MdsUploader {
      override def putFromInputStream(in: InputStream, namespace: String, prefix: Option[String])(
          implicit trace: Traced
      ): MdsPhotoData = {
        MdsPhotoData(namespace, replaceNamePrefix(imageHashGenerator.generateValue, prefix))
      }

      override def putFromArray(photo: Array[Byte], namespace: String)(implicit trace: Traced): MdsPhotoData = {
        MdsPhotoData(namespace, imageHashGenerator.generateValue)
      }

      override def putFromUrl(url: String, namespace: String)(implicit trace: Traced): MdsPhotoData =
        if (!urlDownloaderThrowError.get()) {
          MdsPhotoData(namespace, imageHashGenerator.generateValue)
        } else {
          sys.error("urlDownloader error")
        }

      override def remove(photoId: MdsPhotoData)(implicit trace: Traced): Int = 200

      private def generateFile(): File = {
        val image = imageGenerator.generateValue
        val tmpFile = IO.newTempFile("autoru", "_photo")
        ImageIO.write(image, "jpeg", tmpFile)
        tmpFile
      }

      override def actionWithOrigPhoto[T](
          photoId: MdsPhotoData,
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
          photoId: MdsPhotoData,
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

      override def checkOrigPhotoExist(photoId: MdsPhotoData)(implicit trace: Traced): Boolean = true
    }

    val licensePlateBlur = generateLicensePlateBlur

    val ocrClient = generateOcrClient

    val photoUtils = new PhotoUtils(mdsUploader, licensePlateBlur, ocrClient) {
      override private[utils] def rotate(
          photoId: MdsPhotoData,
          needAngle: Int,
          needNamespace: String,
          checkExistCache: Seq[CheckExistResult] = Seq.empty
      )(implicit trace: Traced): MdsPhotoData = {
        log.info(s"need to rotate image $photoId to angle $needAngle")
        if (needAngle == 0) {
          photoId
        } else {
          MdsPhotoData(needNamespace, replaceNamePrefix(imageHashGenerator.generateValue, photoId.prefix))
        }
      }
    }

    photoUtils
  }

  def generateOcrClient: OcrClient = {
    new OcrClient {
      override def recognizeSts(file: File)(implicit trace: Traced): StsData = {
        StsData(vin = Some(vinGenerator.generateValue))
      }
    }
  }

  def generateLicensePlateBlur: LicensePlateBlur = {
    new LicensePlateBlur {
      override def blur(file: File,
                        blurCoordinates: Option[BlurCoordinates])(implicit trace: Traced): FileBlurResult = {
        val image = imageGenerator.generateValue
        val tmpFile = IO.newTempFile("autoru", "_photo")
        ImageIO.write(image, "jpeg", tmpFile)
        FileBlurResult(tmpFile, Seq.empty)
      }

      override def recognizeNumber(file: File)(implicit trace: Traced): Seq[FileRecognizedNumberResult] = {
        val number = RecognizedNumber.newBuilder().setNumber("A777AA77").setWidthPercent(1).setConfidence(1).build()
        val coords = Array(Array(609L, 879L), Array(609L, 859L), Array(722L, 859L), Array(722L, 879L))
        Seq(FileRecognizedNumberResult(number, Some(BlurCoordinates(coords))))
      }
    }
  }

  def replaceNamePrefix(photoId: String, namePrefix: Option[String]): String = {
    namePrefix.fold(photoId)(prefix => photoId.replaceFirst("-.*_", "-" + prefix + "_"))
  }
}
