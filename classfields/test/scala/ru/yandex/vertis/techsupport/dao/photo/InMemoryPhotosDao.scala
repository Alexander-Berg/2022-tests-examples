package ru.yandex.vertis.vsquality.techsupport.dao.photo

import java.time.Instant

import cats.Monad
import ru.yandex.vertis.vsquality.techsupport.dao.photo.PhotosDao.{PhotoGroupKey, PhotoRecord}
import ru.yandex.vertis.vsquality.techsupport.model.Image

import scala.collection.concurrent.TrieMap

class InMemoryPhotosDao[F[_]: Monad] extends PhotosDao[F] {

  private val storage =
    TrieMap.empty[PhotoGroupKey, Map[Int, (Image, Instant)]]

  override def putPhoto(record: PhotoRecord): F[Unit] = {
    import record._
    val updatedMap =
      storage.get(groupKey) match {
        case Some(innerMap) => innerMap.updated(photoPosition, (photo, photoUploadTime))
        case None           => Map((photoPosition, (photo, photoUploadTime)))
      }

    storage.put(groupKey, updatedMap)

    Monad[F].unit
  }

  override def getPhotos(groupKey: PhotoGroupKey): F[Seq[PhotoRecord]] = {
    val photos =
      storage
        .get(groupKey)
        .fold(Seq.empty[PhotoRecord])(
          _.map { case (position, (photo, instant)) =>
            PhotoRecord(groupKey, position, photo, instant)
          }.toSeq
        )
    Monad[F].pure(photos)
  }

  override def getPhotosByFilter(filter: PhotosDao.Filter): F[Seq[PhotoRecord]] = ???

  override def deletePhotos(groupKey: PhotoGroupKey): F[Unit] = {
    storage.remove(groupKey)
    Monad[F].unit
  }
}
