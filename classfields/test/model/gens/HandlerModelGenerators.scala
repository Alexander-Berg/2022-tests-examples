package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.auto.salesman.SalesmanModel.ApplyProductListToOfferRequest
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.model.user.ApiModel

import scala.collection.JavaConverters._

trait HandlerModelGenerators extends OfferModelGenerators {

  def applyProductListToOfferRequest(): Gen[ApplyProductListToOfferRequest] =
    for {
      offer <- offerGen()
      productNames <- Gen.nonEmptyListOf(
        Gen.oneOf(ProductId.value2alias.values.toSeq)
      )
    } yield
      ApplyProductListToOfferRequest.newBuilder
        .setOffer(offer)
        .addAllProductNames(productNames.asJava)
        .build

  def activeOfferProductsRequestProtoGen(): Gen[ApiModel.ActiveOfferProductsRequest] =
    for {
      offerId <- OfferIdentityGen
      geoId <- Gen.posNum[Long]
    } yield
      ApiModel.ActiveOfferProductsRequest.newBuilder
        .setOfferId(offerId.value)
        .setGeoId(geoId)
        .build

  def activeOffersProductsRequestsProtoGen(): Gen[ApiModel.ActiveOffersProductsRequest] =
    for {
      requests <- Gen.nonEmptyListOf(activeOfferProductsRequestProtoGen())
    } yield
      ApiModel.ActiveOffersProductsRequest.newBuilder
        .addAllActiveOfferProductRequests(requests.asJava)
        .build

}
