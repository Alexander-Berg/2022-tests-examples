package ru.auto.api.managers.shark

import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.BaseSpec
import ru.yandex.vertis.shark.proto.CreditApplication.Payload
import ru.yandex.vertis.shark.proto.CreditApplication.Payload.Autoru
import ru.yandex.vertis.shark.proto.CreditApplication.Payload.Autoru.Offer
import ru.yandex.vertis.shark.proto._

import scala.jdk.CollectionConverters._

class SharkDecayManagerSpec extends BaseSpec {

  private val sharkDecayManager = new SharkDecayManager()

  private val userOffer: Offer = Autoru.Offer
    .newBuilder()
    .setCategory(Category.CARS)
    .setSection(Section.USED)
    .setSellerType(AutoSellerType.PRIVATE)
    .setUserRef("user:123456")
    .build()

  private val dealerOffer: Offer = Autoru.Offer
    .newBuilder()
    .setCategory(Category.CARS)
    .setSection(Section.NEW)
    .setSellerType(AutoSellerType.COMMERCIAL)
    .setUserRef("dealer:654321")
    .build()

  "decay offers" should {
    "user offer" in {
      val creditApplication = generateCreditApplication(Seq(userOffer))
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      val offers = result.getPayload.getAutoru.getOffersList.asScala
      offers.length shouldEqual 1
      offers.head.getUserRef.isEmpty shouldBe true
    }

    "dealer offer" in {
      val creditApplication = generateCreditApplication(Seq(dealerOffer))
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      val offers = result.getPayload.getAutoru.getOffersList.asScala
      offers.length shouldEqual 1
      offers.head.getUserRef shouldEqual dealerOffer.getUserRef
    }

    "user and dealer offers" in {
      val creditApplication = generateCreditApplication(Seq(dealerOffer, userOffer))
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      val offers = result.getPayload.getAutoru.getOffersList.asScala
      offers.length shouldEqual 2
      val withUserRef = offers.filter(_.getUserRef.nonEmpty)
      withUserRef.length shouldEqual 1
      withUserRef.head.getUserRef shouldEqual dealerOffer.getUserRef
    }
  }

  "Credit application decay" should {
    "communication" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder()
        builder.getCommunicationBuilder.getAutoruExternalBuilder
        builder.build
      }
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      result.hasCommunication shouldEqual false
    }

    "notifications" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder()
        builder.addAllNotifications(Seq(Notification.newBuilder.build).asJava)
        builder.build
      }
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      result.getNotificationsList.size shouldEqual 0
    }

    "scores" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder()
        builder.addAllNotifications(Seq(Notification.newBuilder.build).asJava)
        builder.build
      }
      val result = sharkDecayManager.decayCreditApplication(creditApplication)
      val actual = result.getNotificationsList
      actual.size shouldEqual 0
    }
  }

  private def generateCreditApplication(offers: Seq[Offer]): CreditApplication = {
    val autoru: Autoru = Autoru.newBuilder().addAllOffers(offers.asJava).build()
    val payload: Payload = Payload.newBuilder().setAutoru(autoru).build()
    CreditApplication.newBuilder().setPayload(payload).build()
  }
}
