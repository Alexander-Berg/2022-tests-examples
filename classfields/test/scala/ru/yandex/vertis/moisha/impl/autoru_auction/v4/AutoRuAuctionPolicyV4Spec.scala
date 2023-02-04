package ru.yandex.vertis.moisha.impl.autoru_auction.v4

import ru.yandex.vertis.moisha.impl.autoru_auction.AutoRuAuctionPolicy

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AutoRuAuctionPolicyV4Spec extends AutoRuAuctionCallSpec {

  val policy: AutoRuAuctionPolicy = new AutoRuAuctionPolicyV4

}
