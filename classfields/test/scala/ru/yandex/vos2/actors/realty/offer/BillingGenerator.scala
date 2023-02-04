package ru.yandex.vos2.actors.realty.offer

import java.io.{File, FileOutputStream}
import java.util
import java.util.concurrent.ThreadLocalRandom

import org.joda.time.DateTime
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.Good.Placement
import ru.yandex.vertis.billing.Model.OfferBilling.KnownCampaign
import ru.yandex.vertis.billing.Model.OfferId.UserOfferId
import ru.yandex.vertis.billing.Model._

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
object BillingGenerator extends App {
  val offerId = 2350811893728990209L
  val uid = 4815162342L
  val out = "/home/reimai/Documents/jsons/billings.proto"

  def billing(): OfferBilling =
    OfferBilling
      .newBuilder()
      .setOfferId(
        Model.OfferId
          .newBuilder()
          .setVersion(1)
          .setUserOfferId(
            UserOfferId
              .newBuilder()
              .setOfferId("" + offerId)
              .setUser(Model.User.newBuilder().setUid(uid).setVersion(1))
          )
      )
      .setKnownCampaign(
        KnownCampaign
          .newBuilder()
          .setIsActive(true)
          .setActiveDeadline(DateTime.now().plusMinutes(1).getMillis)
          .setCampaign(
            Model.CampaignHeader
              .newBuilder()
              .setVersion(1)
              .setOwner(CustomerHeader.newBuilder().setVersion(1))
              .setOrder(
                Order
                  .newBuilder()
                  .setOwner(CustomerId.newBuilder().setClientId(42).setVersion(1))
                  .setId(offerId)
                  .setVersion(1)
                  .setText("lala")
                  .setCommitAmount(10)
                  .setApproximateAmount(20)
              )
              .setSettings(CampaignSettings.newBuilder().setIsEnabled(true).setVersion(1))
              .setId("cmp_" + ThreadLocalRandom.current().nextLong())
              .setName("'Shut up and take my money'")
              .setProduct(
                Model.Product
                  .newBuilder()
                  .setVersion(1)
                  .addAllGoods(genGoods(false))
              )
          )
      )
      .setVersion(1)
      .build()

  def genGoods(empty: Boolean): java.lang.Iterable[Good] = {
    if (empty) util.Arrays.asList()
    else
      util.Arrays.asList(
        Good
          .newBuilder()
          .setPlacement(
            Placement
              .newBuilder()
              .setCost(
                Cost
                  .newBuilder()
                  .setVersion(1)
              )
          )
          .setVersion(1)
          .build()
      )
  }

  new File(out).delete()
  new File(out).createNewFile()
  billing().writeTo(new FileOutputStream(out))
}
