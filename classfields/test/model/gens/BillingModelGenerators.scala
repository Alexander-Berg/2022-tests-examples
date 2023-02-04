package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.yandex.vertis.billing.Model.OfferBilling.{KnownCampaign, UnknownCampaign}
import ru.yandex.vertis.billing.Model.{BindingSource, OfferBilling}
import ru.yandex.vertis.generators.ProtobufGenerators._
import ru.yandex.vertis.generators.BasicGenerators._

object BillingModelGenerators {

  val bindingSourceGen: Gen[BindingSource] = protoEnum(BindingSource.values())

  val knownCampaignGen: Gen[KnownCampaign] = for {
    campaign <- campaignHeaderGen()
    isActive <- bool
    activeDeadline <- Gen.posNum[Long]
    inactiveReason <- Gen.option(inactiveReasonGen)
    activeStart <- Gen.posNum[Long]
    hold <- readableString
    updateTime <- Gen.posNum[Long]
  } yield {
    val b = KnownCampaign
      .newBuilder()
      .setCampaign(campaign)
      .setIsActive(isActive)
      .setActiveDeadline(activeDeadline)
      .setActiveStart(activeStart)
      .setHold(hold)
      .setUpdateTime(updateTime)
    inactiveReason.foreach(b.setInactiveReason)
    b.build()
  }

  val unknownCampaignGen: Gen[UnknownCampaign] = for {
    campaignId <- readableString
  } yield
    UnknownCampaign
      .newBuilder()
      .setCampaignId(campaignId)
      .build()

  val offerBillingGen: Gen[OfferBilling] = for {
    version <- Gen.posNum[Int]
    timestamp <- Gen.posNum[Long]
    source <- bindingSourceGen
    knownCampaign <- knownCampaignGen
    unknownCampaign <- unknownCampaignGen
  } yield
    OfferBilling
      .newBuilder()
      .setVersion(version)
      .setTimestamp(timestamp)
      .setSource(source)
      .setKnownCampaign(knownCampaign)
      .setUnknownCampaign(unknownCampaign)
      .build()
}
