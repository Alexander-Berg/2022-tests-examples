package ru.yandex.realty.search

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.proto.crypta.EKeywordId._
import ru.yandex.proto.crypta.{EKeywordId, Profile}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.ranking.RankingNumFactor._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class BigBRankingFactorsTest extends SpecBase with OneInstancePerTest with BigBRankingFactors {

  val profile = Profile
    .newBuilder()
    .addAllItems(profileItemsGen())
    .build()

  "BigBRankingFactorsTest" should {

    "correctly extract keyword factors" in {
      extractKeywords(Seq(profile)).toMap shouldBe
        Map(
          AGE_543_1 -> 100500f,
          KEYWORD_601_143 -> 1,
          GENDER_174_1 -> 100500f,
          KEYWORD_547_1024 -> 1,
          UNRECOGNIZED -> 1,
          KEYWORD_602_143 -> 1,
          INCOME_614_1 -> 100500f
        )
    }

    "correctly extract counters" in {
      extractCounters(Seq(profile))
    }

    "correctly extract num factors" in {
      extractBigBNumFactors(Seq(profile))
    }

    "correctly extract num factors without profile " in {
      extractBigBNumFactors(Seq.empty) eq Map.empty
    }
  }

  def profileItemsGen(): java.util.List[Profile.ProfileItem] = {
    List(
      KI_CRYPTA_GENDER,
      KI_CRYPTA_AGE_SEGMENT,
      KI_CRYPTA_USER_INCOME,
      KI_CRYPTA_COMMON_SEGMENTS,
      KI_CRYPTA_LONG_INTEREST,
      KI_CRYPTA_SHORT_INTEREST
    ).map(keywordGen).asJava
  }

  def keywordGen(keywordId: EKeywordId): Profile.ProfileItem = {
    Profile.ProfileItem
      .newBuilder()
      .setKeywordId(keywordId.getNumber)
      .addUintValues(143) // 601, 602
      .addUintValues(1024) // 547
      .addUintValues(88005553535L) // KI_REALTY_OFFERID_VIEWED
      .addWeightedUintValues(
        Profile.WeightedUInt // GENDER, AGE, INCOME
          .newBuilder()
          .setFirst(1)
          .setWeight(100500)
          .build()
      )
      .build()
  }
}
