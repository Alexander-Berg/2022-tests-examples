package ru.yandex.vertis.phonoteka.dao

import java.time.Instant

import cats.instances.list._
import cats.syntax.traverse._
import ru.yandex.vertis.phonoteka.util.YdbSpecBase
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.phonoteka.dao.MetadataDao._
import ru.yandex.vertis.phonoteka.model.metadata.{Metadata, OfMetadata, YandexMoneyMetadata}
import ru.yandex.vertis.phonoteka.util.Clearable._
import ru.yandex.vertis.phonoteka.model.Arbitraries._
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.dao.MetadataDaoSpecBase._
import ru.yandex.vertis.quality.lang_utils.Use

import scala.concurrent.duration._

trait MetadataDaoSpecBase extends YdbSpecBase {
  def dao: MetadataDao[F]

  before {
    dao.clear()
  }

  private val metadata: Metadata = generate[Metadata]()

  "MetadataDao" should {
    "get filtered by phone" in {
      dao.clear()
      val phone2 = generate[Phone]()
      val phone3 = generate[Phone]()
      val metadata2 = metadata.withPhone(phone2)
      val metadata3 = metadata.withPhone(phone3)
      upsert(Seq(metadata, metadata2, metadata3))
      val filter = Filter(phones = Set(metadata.phone, phone2))
      val expected = Set(metadata, metadata2)
      val actual = get(filter)
      actual shouldBe expected
    }

    "get filtered by metadataTypes" in {
      dao.clear()
      val metadata = generate[Metadata]()
      upsert(Seq(metadata))
      val filter = Filter(phones = Set(metadata.phone), metadataTypes = Use(Set(metadata.`type`)))
      val expected = Set(metadata)
      val actual = get(filter)
      actual shouldBe expected
    }

    "get filtered by empty metadataTypes" in {
      dao.clear()
      upsert(Seq(metadata))
      val filter = Filter(phones = Set(metadata.phone), metadataTypes = Use(Set.empty))
      val expected = Set.empty
      val actual = get(filter)
      actual shouldBe expected
    }

    "get filtered by updatedSince" in {
      dao.clear()
      val since = generate[Instant]()
      val phone2 = generate[Phone]()
      val matchedMetadata = metadata.withUpdateTime(since)
      val unmatchedMetadata = metadata.withPhone(phone2).withUpdateTime(since.minusSeconds(1.minute.toSeconds))
      upsert(Seq(matchedMetadata, unmatchedMetadata))
      val filter = Filter(phones = Set(metadata.phone, phone2), updatedSince = Use(since))
      val expected = Set(matchedMetadata)
      val actual = get(filter)
      actual shouldBe expected
    }

    "upsert" in {
      dao.clear()
      val newerMetadata = metadata.withUpdateTime(metadata.updateTime.plusSeconds(1.minute.toSeconds))
      upsert(Seq(metadata, newerMetadata))
      val filter = Filter(phones = Set(metadata.phone))
      val expected = Set(newerMetadata)
      val actual = get(filter)
      actual shouldBe expected
    }
  }

  private def upsert(metadata: Seq[Metadata]): Unit = metadata.map(dao.upsert).toList.sequence.await
  private def get(filter: Filter): Set[Metadata] = dao.get(filter).await
}

object MetadataDaoSpecBase {
  implicit class RichMetadata(val value: Metadata) extends AnyVal {

    def withPhone(phone: Phone): Metadata =
      value match {
        case m: OfMetadata          => m.copy(phone = phone)
        case m: YandexMoneyMetadata => m.copy(phone = phone)
      }

    def withUpdateTime(updateTime: Instant): Metadata =
      value match {
        case m: OfMetadata          => m.copy(updateTime = updateTime)
        case m: YandexMoneyMetadata => m.copy(updateTime = updateTime)
      }
  }
}
