package ru.yandex.vertis.general.gost.logic.testkit

import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.gost.logic.validation.ValidationManager
import ru.yandex.vertis.general.gost.logic.validation.ValidationManager.ValidationManager
import ru.yandex.vertis.general.gost.model.validation.attributes.{AttributeValueRequired, AttributeValueUnexpected}
import ru.yandex.vertis.general.gost.model.validation.fields.TitleRequired
import ru.yandex.vertis.general.gost.model.validation.{Banned, ValidatedEntity, ValidationError}
import ru.yandex.vertis.general.gost.model.{CriticalValidationError, Offer, OfferStatus}
import zio._

object TestValidationManager extends ValidationManager.Service {

  override def validateFields(entity: ValidatedEntity): IO[CriticalValidationError, Seq[ValidationError]] =
    ZIO.succeed {
      if (entity.title == badTitle) {
        Seq(TitleRequired)
      } else if (entity.categoryId.contains(categoryWithRequiredAttribute)) {
        Seq(AttributeValueRequired(missingAttribute, missingAttribute))
      } else if (entity.attributes.contains(invalidAttribute)) {
        Seq(AttributeValueUnexpected(invalidAttribute))
      } else {
        Seq.empty
      }
    }

  override def isBanned(offer: Offer): UIO[Option[Banned]] =
    for {
      banReasonsDictionary <- TestBansDictionaryService.service.flatMap(_.banReasons)
    } yield {
      OfferStatus.asBanned(offer.status).map { banned =>
        Banned(
          banned.banInfo.isInherited,
          banned.banInfo.banReasonCodes.toList
            .flatMap(banReasonsDictionary.get)
        )
      }
    }

  val badTitle = "fail-validation"
  val categoryWithRequiredAttribute = "category_with_required_attribute"
  val invalidAttribute = "invalid_attribute"
  val missingAttribute = "missing_attribute"

  val layer: ULayer[ValidationManager] = ZLayer.succeed(TestValidationManager)
}
