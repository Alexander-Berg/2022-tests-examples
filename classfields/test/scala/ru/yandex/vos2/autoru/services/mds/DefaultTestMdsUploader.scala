package ru.yandex.vos2.autoru.services.mds

import java.io.{File, InputStream}

import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.Photo.CheckExistResult
import ru.yandex.vos2.services.mds.MdsPhotoData

/**
  * Created by sievmi on 06.11.18
  */
object DefaultTestMdsUploader extends MdsUploader {

  override def putFromInputStream(in: InputStream, namespace: String, prefix: Option[String])(
      implicit trace: Traced
  ): MdsPhotoData = {
    MdsPhotoData(namespace, "put-from-input-stream-photo")
  }

  override def putFromArray(photo: Array[Byte], namespace: String)(implicit trace: Traced): MdsPhotoData = {
    MdsPhotoData(namespace, "put-from-array-photo")
  }

  override def putFromUrl(url: String, namespace: String)(implicit trace: Traced): MdsPhotoData = {
    MdsPhotoData(namespace, "put-from-url-photo")
  }

  override def actionWithOrigPhoto[T](
      photoId: MdsPhotoData,
      checkExistCache: Seq[CheckExistResult] = Seq.empty
  )(action: File => T)(implicit trace: Traced): T = {
    applyAction(action)
  }

  override def actionWithPhotoBySize[T](
      photoId: MdsPhotoData,
      size: String,
      checkExistCache: Seq[CheckExistResult] = Seq.empty
  )(action: File => T)(implicit trace: Traced): T = {
    applyAction(action)
  }

  override def remove(photoId: MdsPhotoData)(implicit trace: Traced): Int = 0

  private def applyAction[T](action: File => T): T = {
    val file = generateFile()
    try {
      action(file)
    } finally {
      file.delete()
    }
  }

  private def generateFile(): File = {
    val file = new File("test-file")
    file.createNewFile()
    file
  }

  override def checkOrigPhotoExist(photoId: MdsPhotoData)(implicit trace: Traced): Boolean = {
    true
  }
}
