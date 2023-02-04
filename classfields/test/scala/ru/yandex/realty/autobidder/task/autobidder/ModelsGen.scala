package ru.yandex.realty.autobidder.task.autobidder

import java.util.UUID

import org.scalacheck.Gen
import ru.yandex.realty.autobidder.dao.Status
import ru.yandex.realty.autobidder.model.Strategy
import ru.yandex.realty.autobidder.model.Strategy.{CardWinner, ListingPlace, Premium}
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.util.AuctionCommon
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

/**
  * @author rmuzhikov
  */
trait ModelsGen extends BasicGenerators with ProtobufMessageGenerators with ProtoInstanceProvider {

  def campaignGen(siteId: Option[Long] = Option.empty[Long]): Gen[Campaign] = {
    for {
      siteId <- siteId.map(Gen.const).getOrElse(Gen.posNum[Long])
      ctr <- Gen.chooseNum(1, 100)
      geoId <- Gen.chooseNum(1, 5)
      companyId <- Gen.posNum[Long]
      minimalBid <- Gen.const(60000L)
      bidStep <- Gen.chooseNum(100, 500)
      bid <- Gen.chooseNum(0, 1000).map(mp => bidStep * mp + minimalBid)
      balance <- Gen.chooseNum(0, 1000).map(mp => bidStep * mp + minimalBid)
      campaignId = UUID.randomUUID().toString
      config <- Gen.option(campaignConfigGen)
      hasSuperCall <- Gen.oneOf(true, false)
      isWorkingNow <- Gen.oneOf(true, false)
    } yield Campaign(
      campaignId,
      siteId,
      ctr,
      geoId,
      companyId,
      bid,
      bidStep,
      minimalBid,
      balance,
      hasSuperCall,
      isWorkingNow,
      config.map(c => Campaign.Autobidder(c, Option.empty[Long], Campaign.Autobidder.State.Active))
    )
  }

  def campaignConfigGen: Gen[Campaign.Autobidder.Config] =
    for {
      uid <- Gen.posNum[Long]
      bidLimit <- Gen.posNum[Long]
      strategy <- strategyGen
      status <- Gen.oneOf(Status.values.toSeq)
    } yield Campaign.Autobidder.Config(
      uid,
      bidLimit,
      strategy,
      status
    )

  def strategyGen: Gen[Strategy] =
    Gen.oneOf(
      Gen.const(Strategy.newBuilder().setCardWinner(CardWinner.newBuilder()).build()),
      Gen.const(Strategy.newBuilder().setPremium(Premium.newBuilder()).build()),
      Gen
        .chooseNum(1, 1000)
        .map(place => Strategy.newBuilder().setListingPlace(ListingPlace.newBuilder().setPlace(place)).build())
    )
}
