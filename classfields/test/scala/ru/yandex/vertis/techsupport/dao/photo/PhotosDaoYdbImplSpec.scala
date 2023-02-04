package ru.yandex.vertis.vsquality.techsupport.dao.photo

import java.time.{Instant, LocalDateTime, ZoneOffset}
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.vsquality.techsupport.dao.photo.PhotosDao.{PhotoGroupKey, PhotoRecord}
import ru.yandex.vertis.vsquality.techsupport.model.{Image, ScenarioId, Tags, UserId}
import ru.yandex.vertis.vsquality.techsupport.util.Clearable
import ru.yandex.vertis.vsquality.techsupport.util.Clearable.Ops
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase

import java.time.temporal.ChronoUnit

class PhotosDaoYdbImplSpec extends YdbSpecBase {
  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  private def generateRecord = generate[PhotoRecord]()

  implicit def clearablePhotosDao[C[_]]: Clearable[PhotosDao[C]] =
    () =>
      ydb
        .runTx(
          ydb.execute(
            s"""
               |DELETE FROM uploaded_photos;
               |""".stripMargin
          )
        )
        .void
        .await

  before {
    dao.clear()
  }

  private lazy val dao: PhotosDao[F] = new PhotosDaoYdbImpl(ydb)

  "PhotosDaoYdbImpl" should {
    "store and retrieve photos" in {
      val record = generateRecord
      saveRecord(record)
      val retrievedRecords = dao.getPhotos(record.groupKey).await

      retrievedRecords.size shouldBe 1
      retrievedRecords.head shouldBe record
    }

    "rewrite records" in {
      val record = generateRecord
      val updatedPhoto = Image("http://domain.com".taggedWith[Tags.Url])

      saveRecord(record)
      saveRecord(record.copy(photo = updatedPhoto))

      val retrievedRecords = dao.getPhotos(record.groupKey).await

      retrievedRecords.size shouldBe 1
      retrievedRecords.head.photo shouldBe updatedPhoto
    }

    "select only matched by key" in {
      val records =
        (1 to 5).map { _ =>
          generateRecord
        }
      records.foreach(saveRecord)
      val first = records.head

      val retrievedRecords = dao.getPhotos(first.groupKey).await

      retrievedRecords.size shouldBe 1
      retrievedRecords.head shouldBe first
    }

    "delete uploaded photos" in {
      val scenarioId = ScenarioId.Internal.ProvenOwnerUploadPhotos
      val userId = UserId.Client.Autoru.PrivatePerson(123L.taggedWith[Tags.AutoruPrivatePersonId])
      val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

      val groupKey = PhotoGroupKey(scenarioId, userId, now)

      def photo(position: Int) = {
        PhotoRecord(
          groupKey,
          position,
          Image(s"http://photo$position.com".taggedWith[Tags.Url]),
          now
        )
      }

      val photos = Seq(photo(1), photo(2), photo(3))
      photos.foreach(saveRecord)

      val retrievedRecordsBefore = dao.getPhotos(groupKey).await
      retrievedRecordsBefore should contain theSameElementsAs photos

      dao.deletePhotos(groupKey).await
      val retrievedRecordsAfter = dao.getPhotos(groupKey).await
      retrievedRecordsAfter shouldBe empty
    }

    "select expired records" in {
      val key0 =
        PhotoGroupKey(
          ScenarioId.Internal.ProvenOwnerUploadPhotos,
          UserId.Client.Autoru.PrivatePerson(123L.taggedWith[Tags.AutoruPrivatePersonId]),
          LocalDateTime.of(2020, 2, 2, 8, 50, 0).toInstant(ZoneOffset.UTC)
        )
      val allExpired =
        Seq(
          PhotoRecord(
            key0,
            photoPosition = 1,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 9, 0, 0).toInstant(ZoneOffset.UTC)
          ),
          PhotoRecord(
            key0,
            photoPosition = 2,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 10, 0, 0).toInstant(ZoneOffset.UTC)
          )
        )

      val key1 =
        PhotoGroupKey(
          ScenarioId.Internal.ProvenOwnerUploadPhotos,
          UserId.Client.Autoru.PrivatePerson(124L.taggedWith[Tags.AutoruPrivatePersonId]),
          LocalDateTime.of(2020, 2, 2, 10, 50, 0).toInstant(ZoneOffset.UTC)
        )
      val onlyOneExpired =
        Seq(
          PhotoRecord(
            key1,
            photoPosition = 1,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 10, 55, 0).toInstant(ZoneOffset.UTC)
          ),
          PhotoRecord(
            key1,
            photoPosition = 2,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 13, 0, 0).toInstant(ZoneOffset.UTC)
          )
        )

      val key2 =
        PhotoGroupKey(
          ScenarioId.Internal.ProvenOwnerUploadPhotos,
          UserId.Client.Autoru.PrivatePerson(125L.taggedWith[Tags.AutoruPrivatePersonId]),
          LocalDateTime.of(2020, 2, 2, 14, 50, 0).toInstant(ZoneOffset.UTC)
        )
      val notExpired =
        Seq(
          PhotoRecord(
            key2,
            photoPosition = 1,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 15, 0, 0).toInstant(ZoneOffset.UTC)
          ),
          PhotoRecord(
            key2,
            photoPosition = 2,
            Image("url".taggedWith[Tags.Url]),
            LocalDateTime.of(2020, 2, 2, 16, 25, 0).toInstant(ZoneOffset.UTC)
          )
        )

      (allExpired ++ onlyOneExpired ++ notExpired).foreach(saveRecord)
      val found =
        dao
          .getPhotosByFilter(
            PhotosDao.Filter.Expired(
              until = LocalDateTime.of(2020, 2, 2, 11, 0, 0).toInstant(ZoneOffset.UTC),
              limit = 100
            )
          )
          .await
      found.toSet shouldBe (allExpired ++ onlyOneExpired).toSet

    }
  }

  private def saveRecord(record: PhotoRecord): Unit = {
    dao.putPhoto(record).await
  }
}
