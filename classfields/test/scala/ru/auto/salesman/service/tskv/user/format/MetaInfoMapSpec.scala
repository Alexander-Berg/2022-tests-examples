package ru.auto.salesman.service.tskv.user.format

import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.model.OfferCategories
import ru.auto.salesman.service.tskv.user.domain.MetaInfo._
import ru.auto.salesman.service.tskv.user.format.impl.tskv.MetaInfoMap
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{OfferIdentityGen, OfferModelGenerators}

class MetaInfoMapSpec extends BaseSpec with OfferModelGenerators {
  private val geoIdStr = "213"
  private val geoId = 213

  "MetaInfoMap" should {

    "transform cars meta info" in {
      val meta = OfferMetaInfo(
        OfferIdentityGen.next,
        offerGen().next
      ).copy(
        geoId = geoId,
        category = OfferCategories.Cars,
        section = Section.USED
      )
      MetaInfoMap.toMap(meta) shouldBe Map(
        "category" -> "cars",
        "sub_category" -> "cars",
        "geo_id" -> geoIdStr,
        "section" -> "used"
      )
    }

    "transform commercial meta info" in {
      val meta = OfferMetaInfo(
        OfferIdentityGen.next,
        offerGen().next
      ).copy(
        geoId = geoId,
        category = OfferCategories.Commercial,
        section = Section.NEW
      )
      MetaInfoMap.toMap(meta) shouldBe Map(
        "category" -> "commercial",
        "sub_category" -> "commercial",
        "geo_id" -> geoIdStr,
        "section" -> "new"
      )
    }

    "transform commercial child meta info" in {
      val meta = OfferMetaInfo(
        OfferIdentityGen.next,
        offerGen().next
      ).copy(
        geoId = geoId,
        category = OfferCategories.Bus,
        section = Section.NEW
      )
      MetaInfoMap.toMap(meta) shouldBe Map(
        "category" -> "commercial",
        "sub_category" -> "bus",
        "geo_id" -> geoIdStr,
        "section" -> "new"
      )
    }

    "transform moto meta info" in {
      val meta = OfferMetaInfo(
        OfferIdentityGen.next,
        offerGen().next
      ).copy(
        geoId = geoId,
        category = OfferCategories.Moto,
        section = Section.NEW
      )
      MetaInfoMap.toMap(meta) shouldBe Map(
        "category" -> "moto",
        "sub_category" -> "moto",
        "geo_id" -> geoIdStr,
        "section" -> "new"
      )
    }

    "transform moto child meta info" in {
      val meta = OfferMetaInfo(
        OfferIdentityGen.next,
        offerGen().next
      ).copy(
        geoId = geoId,
        category = OfferCategories.Atv,
        section = Section.NEW
      )
      MetaInfoMap.toMap(meta) shouldBe Map(
        "category" -> "moto",
        "sub_category" -> "atv",
        "geo_id" -> geoIdStr,
        "section" -> "new"
      )
    }
  }

}
