package ru.yandex.realty.managers.cabinet

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.billing.{BillingCampaignStorage, BillingCampaignStorageImpl}
import ru.yandex.realty.sites.CompaniesStorage
import CabinetManagerTestComponents._
import ru.yandex.realty.model.sites.Company
import ru.yandex.vertis.billing.Model.OfferId.ServiceObject
import ru.yandex.vertis.billing.Model.{
  CampaignHeader,
  CampaignSettings,
  CustomerHeader,
  CustomerId,
  OfferBilling,
  OfferId,
  Order,
  Product
}

import scala.collection.JavaConverters._

trait CabinetManagerTestComponents {

  def billingCampaignProvider: Provider[BillingCampaignStorage] = () => billingCampaignStorage

  def companiesProvider: Provider[CompaniesStorage] = () => companiesStorage
}

object CabinetManagerTestComponents {
  val companyWithMaskIncomingPhoneId = "123456"
  val companyCampaignWithMaskIncomingPhoneId = "1"
  val companyWithoutMaskIncomingPhoneId = "123457"
  val companyCampaignWithoutMaskIncomingPhoneId = "2"

  val billingCampaignStorage: BillingCampaignStorageImpl = BillingCampaignStorageImpl(
    Map(
      companyCampaignWithMaskIncomingPhoneId -> buildOfferBilling(
        companyCampaignWithMaskIncomingPhoneId,
        companyWithMaskIncomingPhoneId
      ),
      companyCampaignWithoutMaskIncomingPhoneId -> buildOfferBilling(
        companyCampaignWithoutMaskIncomingPhoneId,
        companyWithoutMaskIncomingPhoneId
      )
    )
  )

  val companiesStorage = new CompaniesStorage(buildCompanies().asJava)

  private def buildOfferBilling(campaignId: String, companyId: String): OfferBilling = {
    val placeholder = 1
    OfferBilling
      .newBuilder()
      .setVersion(placeholder)
      .setOfferId(
        OfferId
          .newBuilder()
          .setVersion(placeholder)
          .setServiceObject(ServiceObject.newBuilder().setPartnerId(companyId))
      )
      .setKnownCampaign(
        OfferBilling.KnownCampaign
          .newBuilder()
          .setCampaign(
            CampaignHeader
              .newBuilder()
              .setVersion(placeholder)
              .setOrder(
                Order
                  .newBuilder()
                  .setVersion(placeholder)
                  .setId(placeholder)
                  .setActText("text")
                  .setCommitAmount(placeholder)
                  .setApproximateAmount(placeholder)
                  .setOwner(CustomerId.newBuilder().setClientId(placeholder).setVersion(placeholder))
                  .setText("text")
              )
              .setId(campaignId)
              .setOwner(CustomerHeader.newBuilder().setVersion(placeholder))
              .setProduct(Product.newBuilder().setVersion(placeholder))
              .setInactiveSince(placeholder)
              .setSettings(CampaignSettings.newBuilder().setVersion(placeholder).setIsEnabled(true))
          )
      )
      .build()

  }

  private def buildCompanies(): Seq[Company] = {
    val companyWithMaskIncomingPhone = new Company(companyWithMaskIncomingPhoneId.toLong)
    companyWithMaskIncomingPhone.setMaskIncomingPhoneNumber(true)
    val companyWithoutMaskIncomingPhone = new Company(companyWithoutMaskIncomingPhoneId.toLong)
    Seq(
      companyWithMaskIncomingPhone,
      companyWithoutMaskIncomingPhone
    )
  }
}
