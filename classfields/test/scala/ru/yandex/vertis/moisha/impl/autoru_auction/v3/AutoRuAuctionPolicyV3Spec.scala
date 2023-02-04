package ru.yandex.vertis.moisha.impl.autoru_auction.v3

import ru.yandex.vertis.moisha.impl.autoru_auction.AutoRuAuctionPolicy

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AutoRuAuctionPolicyV3Spec extends AutoRuAuctionCallSpec {

  val policy: AutoRuAuctionPolicy = new AutoRuAuctionPolicyV3

}
