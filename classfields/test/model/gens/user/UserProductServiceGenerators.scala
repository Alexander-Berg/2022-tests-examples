package ru.auto.salesman.test.model.gens.user

import org.scalacheck.Gen
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.{ProductContext, ProductType}
import ru.auto.salesman.model.user.product.Products.{Bundle, Goods, Subscription}
import ru.auto.salesman.model.DomainAware
import ru.auto.salesman.service.user.UserProductService
import ru.auto.salesman.service.user.UserProductService.{
  ActiveOfferProductsRequest,
  ProductResponses
}
import ru.yandex.vertis.generators.DateTimeGenerators
import ru.auto.salesman.model.user.product.AutoruProduct

trait UserProductServiceGenerators
    extends UserModelGenerators
    with DateTimeGenerators
    with DomainAware {

  def productResponseGen(
      product: Gen[AutoruProduct] = ProductGen,
      offerId: Gen[OfferIdentity] = OfferIdentityGen,
      productContext: Gen[ProductContext] = productContextGen(
        Gen.oneOf(ProductType.values)
      )
  ): Gen[UserProductService.Response] =
    for {
      id <- Gen.posNum[Int].map(_.toString)
      offerId <- offerId
      user <- AutoruUserIdGen
      product <- product
      amount <- FundsGen
      status <- productStatusGen
      transactionId <- readableString
      context <- productContext
      interval <- dateTimeIntervalGen
      activated = interval.from
      deadline = interval.to
      prolongable <- ProlongableGen
      counter <- Gen.posNum[Long]
    } yield
      product match {
        case _: Goods =>
          UserProductService.Response.Goods(
            id,
            offerId,
            user,
            product,
            amount,
            status,
            transactionId,
            context,
            activated,
            deadline,
            prolongable
          )
        case _: Bundle =>
          UserProductService.Response.Bundle(
            id,
            offerId,
            user,
            product,
            amount,
            status,
            transactionId,
            context,
            activated,
            deadline,
            prolongable
          )
        case _: Subscription =>
          UserProductService.Response.Subscription(
            id,
            user,
            product,
            counter,
            amount,
            status,
            transactionId,
            context,
            activated,
            deadline,
            prolongable
          )
      }

  def productsResponsesGen(): Gen[ProductResponses] =
    Gen.nonEmptyListOf(productResponseGen()).map(ProductResponses)

  def activeOfferProductsRequestGen(): Gen[ActiveOfferProductsRequest] =
    for {
      offerId <- OfferIdentityGen
      geoId <- Gen.posNum[Long]
    } yield ActiveOfferProductsRequest(offerId, geoId)
}
