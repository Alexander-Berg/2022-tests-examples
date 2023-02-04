package ru.yandex.vertis.clustering.services

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers

@RunWith(classOf[JUnitRunner])
class FactsFilterSpec extends BaseSpec {

  private val realtyFilter = FactsFilter.forDomain(Domains.Realty)
  private val autoruFilter = FactsFilter.forDomain(Domains.Autoru)

  private val realtyUser = User(Domains.Realty, "123")
  private val autoruUser = User(Domains.Autoru, "123")

  "RealtyFactsFilter" should {

    "Valid fact" in {
      val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "all right!", None).get
      val fact = Fact(realtyUser, feature, DateTimeUtils.now)
      realtyFilter(fact) shouldBe true
    }

    "DeviceUid nobody" in {
      val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "nobody", None).get
      val fact = Fact(realtyUser, feature, DateTimeUtils.now)
      realtyFilter(fact) shouldBe false
    }

    "Fact with unsupported domain" in {
      val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "all right!", None).get
      val fact = Fact(autoruUser, feature, DateTimeUtils.now)
      realtyFilter(fact) shouldBe false
    }

    "Not null Gaid" in {
      val feature = FeatureHelpers.parse(FeatureTypes.GaidType, "f370f3f2-4b40-4b15-8023-4baf0ab31620", None).get
      val fact = Fact(realtyUser, feature, DateTimeUtils.now)
      realtyFilter(fact) shouldBe true
    }

    "Null Gaid" in {
      val feature = FeatureHelpers.parse(FeatureTypes.GaidType, "00000000-0000-0000-0000-000000000000", None).get
      val fact = Fact(realtyUser, feature, DateTimeUtils.now)
      realtyFilter(fact) shouldBe false
    }
  }

  "AutoruyFactsFilter" should {

    "Valid fact" in {
      val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "all right!", None).get
      val fact = Fact(autoruUser, feature, DateTimeUtils.now)
      autoruFilter(fact) shouldBe true
    }

    "forbiddenAutoruIds" in {
      val forbiddenAutoruIds = Seq("0", "54")
      val result = forbiddenAutoruIds.exists { userId =>
        val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "all right!", None).get
        val user = User(Domains.Autoru, userId)
        val fact = Fact(user, feature, DateTimeUtils.now)
        autoruFilter(fact)
      }
      result shouldBe false
    }

    "Fact with unsupported domain" in {
      val feature = FeatureHelpers.parse(FeatureTypes.DeviceUidType, "all right!", None).get
      val fact = Fact(realtyUser, feature, DateTimeUtils.now)
      autoruFilter(fact) shouldBe false
    }

    "Not null Gaid" in {
      val feature = FeatureHelpers.parse(FeatureTypes.GaidType, "f370f3f2-4b40-4b15-8023-4baf0ab31620", None).get
      val fact = Fact(autoruUser, feature, DateTimeUtils.now)
      autoruFilter(fact) shouldBe true
    }

    "Null Gaid" in {
      val feature = FeatureHelpers.parse(FeatureTypes.GaidType, "00000000-0000-0000-0000-000000000000", None).get
      val fact = Fact(autoruUser, feature, DateTimeUtils.now)
      autoruFilter(fact) shouldBe false
    }
  }
}
