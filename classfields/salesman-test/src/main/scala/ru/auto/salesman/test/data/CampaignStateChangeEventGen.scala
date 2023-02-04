package ru.auto.salesman.test.data

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import ru.yandex.vertis.billing.BillingEvent.CampaignStateChangeEvent
import ru.yandex.vertis.billing.Model.{CampaignHeader, CustomerHeader}
import ru.yandex.vertis.billing.Model

object CampaignStateChangeEventGen {

  def apply: Gen[CampaignStateChangeEvent] =
    for {
      version <- arbitrary[Int]
      customerId <- arbitrary[Long].map { clientId =>
        Model.CustomerId.newBuilder().setClientId(clientId).setVersion(version)
      }
      customerHeader = CustomerHeader
        .newBuilder()
        .setId(customerId)
        .setVersion(version)
      order <- for {
        id <- arbitrary[Long]
        text <- Gen.alphaNumStr
        commitAmount <- arbitrary[Long]
        approximateAmount <- arbitrary[Long]
      } yield
        Model.Order
          .newBuilder()
          .setVersion(version)
          .setId(id)
          .setOwner(customerId)
          .setText(text)
          .setCommitAmount(commitAmount)
          .setApproximateAmount(approximateAmount)
      product = Model.Product.newBuilder().setVersion(version)
      settings = Model.CampaignSettings
        .newBuilder()
        .setVersion(version)
        .setIsEnabled(true)
      campaignHeader = CampaignHeader
        .newBuilder()
        .setId("")
        .setVersion(version)
        .setOwner(customerHeader)
        .setOrder(order)
        .setProduct(product)
        .setSettings(settings)
    } yield
      CampaignStateChangeEvent
        .newBuilder()
        .setCampaignHeader(campaignHeader)
        .build()
}
