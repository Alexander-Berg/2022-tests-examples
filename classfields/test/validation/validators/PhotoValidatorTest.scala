package ru.yandex.vertis.general.gost.logic.test.validation.validators

import general.feed.model.FeedSourceEnum.FeedSource
import general.feed.transformer.FeedFormat
import general.gost.feed_api.NamespaceId
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit
import zio.test._
import zio.test.Assertion._
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.{validatorTest, Photo}
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.PhotoValidator
import ru.yandex.vertis.general.gost.model.Offer.{ExternalOfferId, FeedInfo}
import ru.yandex.vertis.general.gost.model.OfferOrigin
import ru.yandex.vertis.general.gost.model.Photo.MdsImage
import ru.yandex.vertis.general.gost.model.validation.fields.{IllegalMdsPhoto, PhotoRequired, TooManyPhotos}
import zio.ZLayer

object PhotoValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PhotoValidator")(
      validatorTest("Fail when photos are empty and source is Form")(
        _.copy(photos = Seq.empty, origin = OfferOrigin.Form)
      )(contains(PhotoRequired(None))),
      validatorTest("Fail when photos are empty and source is Feed")(
        _.copy(photos = Seq.empty, origin = OfferOrigin.Feed)
      )(contains(PhotoRequired(None))),
      validatorTest("Work fine when photos are empty and source is Parsed Feed")(
        _.copy(
          photos = Seq.empty,
          origin = OfferOrigin.Feed,
          feedInfo = Some(
            FeedInfo(
              namespaceId = Some(NamespaceId("avito-parsed")),
              externalId = ExternalOfferId("someId"),
              taskId = 11,
              rawOffer = None,
              source = FeedSource.FEED
            )
          ),
          updatingFrom = OfferOrigin.Feed
        )
      )(isEmpty),
      validatorTest("Fail when photos are empty and source is Parsed Feed updating through form")(
        _.copy(
          photos = Seq.empty,
          origin = OfferOrigin.Feed,
          feedInfo = Some(
            FeedInfo(
              namespaceId = Some(NamespaceId("avito-parsed")),
              externalId = ExternalOfferId("someId"),
              taskId = 11,
              rawOffer = None,
              source = FeedSource.FEED
            )
          ),
          updatingFrom = OfferOrigin.Form
        )
      )(contains(PhotoRequired(None))),
      validatorTest("Fail when has more than maximum photos number")(
        _.copy(photos = Seq.fill(PhotoValidator.MaximumPhotosNum + 1)(Photo))
      )(contains(TooManyPhotos(PhotoValidator.MaximumPhotosNum + 1, PhotoValidator.MaximumPhotosNum))),
      validatorTest("Fail when namespace is not o-yandex")(entity =>
        entity.copy(photos =
          List(
            ValidatorTestkit.Photo.copy(mds = MdsImage(namespace = "wrong-namespace", name = "aba32cd", groupId = 123))
          )
        )
      )(contains(IllegalMdsPhoto)),
      validatorTest("Fail when name is not alphanumerical or hyphen")(entity =>
        entity.copy(photos =
          List(
            ValidatorTestkit.Photo.copy(mds = MdsImage(namespace = "o-yandex", name = "aba<asd $,", groupId = 123))
          )
        )
      )(contains(IllegalMdsPhoto)),
      validatorTest("Work fine with hyphens in photo name")(entity =>
        entity.copy(photos =
          List(
            ValidatorTestkit.Photo.copy(mds =
              MdsImage(namespace = "o-yandex", name = "6c4890bf-83b6-4100-9fd5-a8007a1ecadc", groupId = 123)
            )
          )
        )
      )(isEmpty),
      validatorTest("Work fine with valid entity")(identity)(isEmpty)
    ).provideCustomLayerShared(ZLayer.succeed[Validator](PhotoValidator))
}
