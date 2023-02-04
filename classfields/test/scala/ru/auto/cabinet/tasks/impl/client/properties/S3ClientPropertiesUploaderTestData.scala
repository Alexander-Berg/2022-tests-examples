package ru.auto.cabinet.tasks.impl.client.properties

import java.time.OffsetDateTime

import ru.auto.cabinet.model.{Client, ClientProperties, ClientStatuses}
import ru.yandex.vertis.billing.Model._

object S3ClientPropertiesUploaderTestData {

  def clients() = {
    val client = Client(
      20101,
      0,
      ClientProperties(
        1,
        1,
        "123",
        ClientStatuses.Active,
        OffsetDateTime.now(),
        "",
        None,
        "",
        None,
        None,
        createdDate = None,
        multipostingEnabled = true,
        firstModerated = false,
        isAgent = false
      )
    )

    List(client, client.copy(clientId = 16453), client.copy(clientId = 782))
  }

  def createCampaignHeader(isActive: Boolean): CampaignHeader = {
    val builder = CampaignHeader
      .newBuilder()
      .setId("123")
      .setSettings(
        CampaignSettings.newBuilder().setIsEnabled(true).setVersion(1))
      .setVersion(1)
      .setOwner(CustomerHeader.newBuilder().setVersion(1).build())
      .setProduct(Product.newBuilder().setVersion(1).build())
      .setOrder(
        Order
          .newBuilder()
          .setId(1)
          .setOwner(CustomerId.newBuilder().setClientId(1).setVersion(1))
          .setVersion(1)
          .setText("1")
          .setCommitAmount(1)
          .setApproximateAmount(2)
          .build())

    if (!isActive) builder.setInactiveReason(InactiveReason.NO_ENOUGH_FUNDS)
    builder.build()
  }

}
