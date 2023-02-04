package ru.auto.salesman.service.tskv.dealer.logger

import ru.auto.api.ApiOfferModel
import ru.auto.salesman.model.{
  BalanceClientId,
  ClientId,
  CompanyId,
  FeatureInstanceId,
  FeatureType,
  Funds,
  OfferCategory,
  OfferId,
  ProductId,
  RegionId,
  TransactionId
}
import ru.auto.salesman.service.BillingEventOptions
import ru.auto.salesman.service.tskv.dealer.format.TskvLogFormat

package object impl {

  case class Request()

  case class Response() extends BillingEventOptions {
    def price: Option[Funds] = None
    def actualPrice: Option[Funds] = None
    def holdId: Option[TransactionId] = None
    def clientId: Option[ClientId] = None
    def agencyId: Option[BalanceClientId] = None
    def companyId: Option[CompanyId] = None
    def regionId: Option[RegionId] = None
  }

  implicit object TestTskvLogFormat$ extends TskvLogFormat[Request, Response] {
    def salesmanComponent(input: Request): String = "salesman-test"

    def actionName(result: Response): String = "action"

    def offerId(input: Request): Option[OfferId] = None

    def category(input: Request): Option[OfferCategory] = None

    def section(input: Request): Option[ApiOfferModel.Section] = None

    def product(input: Request): ProductId = ProductId.Call

    def featureId(result: Response): Option[FeatureInstanceId] = None

    def featureType(result: Response): Option[FeatureType] = None

    def featureAmount(result: Response): Option[Funds] = None

    def loyaltyDiscountId(result: Response): Option[FeatureInstanceId] = None

    def loyaltyDiscountAmount(result: Response): Option[Funds] = None

    def quotaSize(input: Request): Option[Int] = None

    def isQuotaProduct(input: Request): Boolean = true

    def clientId(result: Response): Option[ClientId] = None

    def vin(input: Request): Option[String] = None

    def deliveryRegionsIds(result: Response): Option[String] = None

    def deliveryRegionsPrice(result: Response): Option[Long] = None

    def skipRow(result: Response): Boolean = false
  }

}
