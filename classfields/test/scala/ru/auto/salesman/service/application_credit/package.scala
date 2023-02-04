package ru.auto.salesman.service

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.model.{
  AdsRequestType,
  AdsRequestTypes,
  BalanceClientId,
  CityId,
  DetailedClient,
  OrderId,
  RegionId
}
import ru.auto.salesman.test.ZIOValues
import ru.auto.salesman.util.HasRequestContext
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.{CustomerHeader, InactiveReason}
import zio.blocking.Blocking
import zio.test.environment.{TestClock, TestEnvironment}

import java.time.{Instant, OffsetDateTime, ZoneId}
import scala.collection.JavaConverters._

package object application_credit {

  object ZioTestUtil extends ZIOValues {

    def runTestWithFixedTime[T](dateTime: DateTime)(
        test: => Task[T]
    ): T = {
      val instant = Instant.ofEpochMilli(dateTime.getMillis)
      val zone = ZoneId.of(dateTime.getZone.getID)
      val offset = OffsetDateTime.ofInstant(instant, zone)
      TestClock
        .setDateTime(offset)
        .flatMap(_ => test)
        .provideSomeLayer[Blocking with HasRequestContext](
          zio.ZEnv.live >>> TestEnvironment.live
        )
        .success
        .value
    }

  }

  def buildDetailedClient(
      dealerId: Long = 1,
      balanceClientId: BalanceClientId = 1,
      balanceAgencyId: Option[BalanceClientId] = None,
      orderId: OrderId = 1,
      singlePayment: Set[AdsRequestType] = Set(AdsRequestTypes.CarsUsed)
  ): DetailedClient =
    DetailedClient(
      dealerId,
      None,
      balanceClientId,
      balanceAgencyId,
      None,
      None,
      RegionId(1L),
      CityId(1123L),
      orderId,
      isActive = true,
      firstModerated = true,
      singlePayment = singlePayment
    )

  def buildGood(customId: String): Model.Good = {
    val cost = Model.Cost
      .newBuilder()
      .setVersion(1)
      .setPerCall(Model.Cost.PerCall.newBuilder.setUnits(222L))
      .build()

    val custom = Model.Good.Custom.newBuilder
      .setId(customId)
      .setCost(cost)
      .build()

    val good =
      Model.Good
        .newBuilder()
        .setVersion(1)
        .setCustom(custom)
        .build()

    good
  }

  def buildProduct(goods: List[Model.Good]): Model.Product =
    Model.Product.newBuilder
      .setVersion(1)
      .addAllGoods(goods.asJava)
      .build()

  def buildOwner(ownerId: BalanceClientId): CustomerHeader =
    Model.CustomerHeader.newBuilder
      .setVersion(1)
      .setId(Model.CustomerId.newBuilder().setVersion(1).setClientId(ownerId))
      .build()

  def buildActiveCampaign(
      product: Model.Product,
      owner: CustomerHeader
  ): Model.CampaignHeader =
    buildCampaign(product, owner, inactiveReason = None)

  def buildInactiveCampaign(
      product: Model.Product,
      owner: CustomerHeader,
      inactiveReason: InactiveReason
  ): Model.CampaignHeader =
    buildCampaign(product, owner, Some(inactiveReason))

  private def buildCampaign(
      product: Model.Product,
      owner: CustomerHeader,
      inactiveReason: Option[InactiveReason]
  ): Model.CampaignHeader = {
    val customerId = Model.CustomerId
      .newBuilder()
      .setVersion(1)
      .setClientId(100500)
      .setAgencyId(100500)
      .build()

    val order = Model.Order.newBuilder
      .setVersion(1)
      .setOwner(customerId)
      .setId(100500)
      .setText("order")
      .setCommitAmount(5L)
      .setApproximateAmount(0L)
      .build

    val settings = Model.CampaignSettings
      .newBuilder()
      .setVersion(1)
      .setIsEnabled(true)
      .build()

    val headerBuilder = Model.CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setProduct(product)
      .setOwner(owner)
      .setOrder(order)
      .setSettings(settings)
      .setId("campaignId")
    inactiveReason.foreach(headerBuilder.setInactiveReason)
    headerBuilder.build()
  }

}
