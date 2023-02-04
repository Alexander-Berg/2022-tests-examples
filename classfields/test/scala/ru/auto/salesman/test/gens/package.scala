package ru.auto.salesman.test

import org.scalacheck.Gen
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service.goods.domain.{GoodsDetails, GoodsRequest}
import ru.auto.salesman.test.model.gens.{
  AutoruOfferIdGen,
  GoodsNameGen,
  OfferCategoryGen,
  OfferSectionGen,
  ProductIdGen
}
import ru.yandex.vertis.util.time.DateTimeUtil

package object gens {

  def goodsRequestGen(
      offerIdentityGen: Gen[AutoruOfferId] = AutoruOfferIdGen,
      productGen: Gen[ProductId] = ProductIdGen
  ): Gen[GoodsRequest] =
    for {
      offerId <- offerIdentityGen
      category <- OfferCategoryGen
      section <- OfferSectionGen
      product <- productGen
      possibleBadges <- Gen.listOfN(
        3,
        Gen.oneOf("Бейдж1", "Бейдж2", "Бейдж3", "Бейдж4", "Бейдж5")
      )
      badges = if (product == ProductId.Badge) possibleBadges.toSet else Set.empty[String]
    } yield GoodsRequest(offerId, category, section, product, badges)

  val GoodDetailsGen: Gen[GoodsDetails] = for {
    offerId <- Gen.posNum[Long]
    offerHash <- Gen.alphaStr
    category <- OfferCategoryGen
    product <- GoodsNameGen
    createDate = DateTimeUtil.now
    expireDate = createDate.plusDays(1)
  } yield
    GoodsDetails(
      offerId = offerId,
      category = category,
      product = product,
      createDate = createDate,
      expireDate = Some(expireDate),
      badge = if (product == ProductId.Badge) Some("test-badge") else None,
      offerHash = Some(offerHash)
    )

}
