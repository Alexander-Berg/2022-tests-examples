package ru.yandex.vertis.general.gost.logic.testkit

import common.geobase.model.RegionIds.RegionId
import general.bonsai.category_model.Category
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.bonsai.public.Constants.{odezhdaCategoryId, rabotaCategoryId}
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.model.Offer.{Contact, Video}
import ru.yandex.vertis.general.gost.model.Photo._
import ru.yandex.vertis.general.gost.model.attributes.Attributes
import ru.yandex.vertis.general.gost.model.validation.{ValidatedEntity, ValidationError}
import ru.yandex.vertis.general.gost.model._
import zio.test.{Assertion, _}
import zio.{Has, ZIO}

object ValidatorTestkit {

  val Photo: CompletePhoto =
    CompletePhoto(MdsImage(name = "name", namespace = "o-yandex", groupId = 777), None, Some(PhotoMeta("abc1")), None)

  val UserId = 77777L
  val UserPhone = "+79999999999"
  val ForeignUserPhone = "+994-9999-999-99"

  val ValidEntity: ValidatedEntity = ValidatedEntity(
    entityId = "id",
    entityType = ValidatedEntity.Draft,
    origin = OfferOrigin.Form,
    title = "title",
    description = "description",
    categoryId = None,
    attributes = Attributes.empty,
    photos = Seq(Photo),
    video = Some(Video(url = "https://youtube.com/video_s_kotikami")),
    price = Price.InCurrency(100L),
    addresses = Seq(
      SellingAddress(
        geopoint = SellingAddress.GeoPoint(40.5, 43.2),
        address = None,
        metroStation = None,
        district = None,
        region = Some(SellingAddress.RegionInfo(RegionId(77), isEnriched = false, name = "region", isTown = Some(true)))
      )
    ),
    contacts = Seq(Contact(phone = Some(UserPhone))),
    preferredWayToContact = WayToContact.PhoneCall,
    userId = Some(UserId),
    condition = Some(Offer.Used),
    isPhoneRedirectEnabled = Some(true),
    delivery = None,
    feedInfo = None,
    updatingFrom = OfferOrigin.Form,
    hideOnService = false
  )

  val category: Category = Category()
  val odezhdaCategory: Category = Category(odezhdaCategoryId)
  val rabotaCategory: Category = Category(rabotaCategoryId)

  val bonsaiSnapshot: BonsaiSnapshot =
    BonsaiSnapshot(Seq(Category(rabotaCategoryId), Category(odezhdaCategoryId)), Seq.empty)

  def validatorTest(
      testLabel: String,
      offerCategory: Category = category
    )(validEntityMapping: ValidatedEntity => ValidatedEntity
    )(assertions: Assertion[Seq[ValidationError]]): ZSpec[Has[Validator], CriticalValidationError] =
    testM(testLabel) {
      for {
        validator <- ZIO.service[Validator]
        entity = validEntityMapping(ValidEntity)
        validationErrors <- validator.validate(entity, offerCategory, bonsaiSnapshot).map(_.toList)
      } yield assert(validationErrors)(assertions)
    }
}
