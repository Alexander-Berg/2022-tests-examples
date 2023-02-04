package ru.yandex.vertis.general.wizard

import java.time.Instant

import common.geobase.model.RegionIds.RegionId
import ru.yandex.vertis.general.wizard.model.{
  Attribute,
  MicroOffer,
  OfferSource,
  Price,
  StockOffer,
  StockOfferWithCtr,
  Url
}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen

object Generators {

  // zio.test.magnolia can't derive for estatico.newtype without this :(
  implicit val regionIdDeriveGen: DeriveGen[RegionId] = new DeriveGen[RegionId] {
    override def derive: Gen[Random with Sized, RegionId] = Gen.anyLong.map(RegionId(_))
  }

  implicit val strGen: Gen[Random with Sized, String] = Gen.alphaNumericString.filter(_.nonEmpty)

  val stockOfferWithCtrGen: Gen[Random with Sized, StockOfferWithCtr] = {
    for {
      title <- Gen.alphaNumericString.filter(_.nonEmpty)
      description <- Gen.alphaNumericString.filter(_.nonEmpty)
      address <- Gen.alphaNumericString.filter(_.nonEmpty)
      offerId <- Gen.alphaNumericString.filter(_.nonEmpty)
      image <- Gen.alphaNumericString.filter(_.nonEmpty)
      imageUrl260x194 <- Gen.alphaNumericString.filter(_.nonEmpty)
      imageUrl312x312 <- Gen.alphaNumericString.filter(_.nonEmpty)
      categoryId <- Gen.alphaNumericString.filter(_.nonEmpty)
      price <- DeriveGen[Option[Price]]
      isNew <- DeriveGen[Option[Boolean]]
      images <- DeriveGen[Seq[Url]]
      isMordaApproved <- Gen.boolean
      isYanApproved <- Gen.boolean
      regionIds <- DeriveGen[Seq[RegionId]]
      attributes <- DeriveGen[Seq[Attribute]]
      source <- DeriveGen[OfferSource]
      createdAt <- DeriveGen[Instant]
      shows <- DeriveGen[Option[BigDecimal]]
      cont <- DeriveGen[Option[BigDecimal]]
    } yield StockOfferWithCtr(
      StockOffer(
        title,
        description,
        address,
        offerId,
        price,
        isNew,
        image,
        imageUrl260x194,
        imageUrl312x312,
        images,
        categoryId,
        isMordaApproved,
        isYanApproved,
        regionIds,
        attributes,
        source,
        createdAt
      ),
      shows,
      cont
    )
  }

}
