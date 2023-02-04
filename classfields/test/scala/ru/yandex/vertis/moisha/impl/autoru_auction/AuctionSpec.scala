package ru.yandex.vertis.moisha.impl.autoru_auction

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moisha.impl.autoru_auction.AutoRuAuctionPolicy.AutoRuAuctionRequest
import ru.yandex.vertis.moisha.impl.autoru_auction.gens.autoRuAuctionRequestGen
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Categories.Cars
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Products.Call
import ru.yandex.vertis.moisha.impl.autoru_auction.model.Sections.New
import ru.yandex.vertis.moisha.impl.autoru_auction.model.gens.{contextGen, offerGen}
import ru.yandex.vertis.moisha.impl.autoru_auction.utils.Mark
import ru.yandex.vertis.moisha.model.{Funds, RegionId}
import ru.yandex.vertis.moisha.model.gens.dateTimeIntervalStartsAfterGen
import ru.yandex.vertis.moisha.test.BaseSpec

trait AuctionSpec extends BaseSpec {
  val policy: AutoRuAuctionPolicy
  val start: DateTime

  def req(regionId: Gen[RegionId], mark: Gen[Mark], hasPriorityPlacement: Gen[Boolean]): Gen[AutoRuAuctionRequest] = {
    autoRuAuctionRequestGen(
      product = Call,
      offer = offerGen(category = Cars, section = New),
      context = contextGen(regionId = regionId, cityId = None, marks = mark.map { m =>
        List(m.name)
      }, hasPriorityPlacement = hasPriorityPlacement),
      interval = dateTimeIntervalStartsAfterGen(start)
    )
  }

  def checkPrice(requestGen: Gen[AutoRuAuctionRequest], priceInKopeks: Funds) = {
    forAll(requestGen) { request =>
      val res = policy.estimate(request).success.value
      res.points.head.product.total shouldBe priceInKopeks
    }
  }
}
