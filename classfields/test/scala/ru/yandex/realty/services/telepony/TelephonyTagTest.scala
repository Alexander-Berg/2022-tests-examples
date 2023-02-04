package ru.yandex.realty.services.telepony

import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.features.Features
import ru.yandex.realty.model.offer._
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.sites.SitesGroupingService

import scala.collection.JavaConverters._

class TelephonyTagTest extends FlatSpec with MockFactory {

  private def checkEqual(o: Offer): Unit = {
    val sitesService = mock[SitesGroupingService]
    val features = mock[Features]
    val telephonyTagBuilder = new TelephonyTagBuilder(sitesService, features)
    assert(telephonyTagBuilder.build(o).value === TelephonyTagTest.getTag(o))
  }

  private def makeOffer(
    subjectFederationIdAndRgid: (Int, Long),
    isCertified: Boolean,
    offerType: OfferType,
    categoryType: CategoryType,
    flatType: FlatType,
    salesAgentCategory: SalesAgentCategory,
    commercialType: CommercialType,
    offerId: Int
  ) = {
    val o = new Offer

    val loc = new Location()
    loc.setSubjectFederation(subjectFederationIdAndRgid._1, subjectFederationIdAndRgid._2)
    o.setLocation(loc)

    o.setOfferType(offerType)

    o.setCategoryType(categoryType)

    val apInfo = new ApartmentInfo
    apInfo.setFlatType(flatType)
    o.setApartmentInfo(apInfo)

    val agent = o.createAndGetSaleAgent()
    agent.setCategory(salesAgentCategory)

    val commInfo = new CommercialInfo
    commInfo.setCommercialType(commercialType)
    o.setCommercialInfo(commInfo)

    o.setId(offerId)

    o
  }

  private def setTransactionToOffer(
    o: Offer,
    currency: Currency,
    value: Long,
    pricingPeriod: PricingPeriod,
    areaUnit: AreaUnit
  ): Unit = {
    val trans = new Transaction
    val prInfo = PriceInfo.create(
      Money.of(
        currency,
        value
      ),
      pricingPeriod,
      areaUnit
    )
    trans.setAreaPrice(prInfo, null)
    o.setTransaction(trans)
  }

  "Telephony tag" should "process trivial case without certification" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = false,
      OfferType.SELL,
      CategoryType.GARAGE,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process case with UNKNOWN fieleds" in {
    val o = makeOffer(
      (Regions.MOSCOW, NodeRgid.MOSCOW),
      isCertified = true,
      OfferType.UNKNOWN,
      CategoryType.UNKNOWN,
      FlatType.UNKNOWN,
      SalesAgentCategory.UNKNOWN,
      CommercialType.UNKNOWN,
      42
    )

    checkEqual(o)
  }

  it should "process rent per month" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = true,
      OfferType.RENT,
      CategoryType.HOUSE,
      FlatType.UNKNOWN,
      SalesAgentCategory.AGENCY,
      CommercialType.UNKNOWN,
      42
    )

    setTransactionToOffer(o, Currency.EUR, 1337, PricingPeriod.PER_MONTH, AreaUnit.SQUARE_METER)

    checkEqual(o)
  }

  it should "process Srp without LO" in {
    val o = makeOffer(
      (Regions.SPB, NodeRgid.SPB),
      isCertified = false,
      OfferType.SELL,
      CategoryType.COMMERCIAL,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process trivial cases with certification" in {
    val o = makeOffer(
      (Regions.KAZAN, NodeRgid.KAZAN),
      isCertified = true,
      OfferType.SELL,
      CategoryType.GARAGE,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process rent per day" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = true,
      OfferType.RENT,
      CategoryType.HOUSE,
      FlatType.UNKNOWN,
      SalesAgentCategory.AGENCY,
      CommercialType.UNKNOWN,
      42
    )

    setTransactionToOffer(o, Currency.EUR, 1337, PricingPeriod.PER_DAY, AreaUnit.SQUARE_METER)

    checkEqual(o)
  }

  it should "process selling apartments in Spb without certification" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = false,
      OfferType.SELL,
      CategoryType.APARTMENT,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process Spb with certification" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = true,
      OfferType.SELL,
      CategoryType.GARAGE,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process Spb with Commercial CategotyType" in {
    val o = makeOffer(
      (Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST),
      isCertified = false,
      OfferType.UNKNOWN,
      CategoryType.COMMERCIAL,
      FlatType.NEW_FLAT,
      SalesAgentCategory.OWNER,
      CommercialType.LAND,
      42
    )

    checkEqual(o)
  }

  it should "process rent per day without certification" in {
    val o = makeOffer(
      (Regions.CHELYABINSKAYA_OBLAST, NodeRgid.CHELYABINSKAYA_OBLAST),
      isCertified = false,
      OfferType.RENT,
      CategoryType.HOUSE,
      FlatType.UNKNOWN,
      SalesAgentCategory.AGENCY,
      CommercialType.UNKNOWN,
      42
    )

    setTransactionToOffer(o, Currency.EUR, 1337, PricingPeriod.PER_DAY, AreaUnit.SQUARE_METER)

    checkEqual(o)
  }
}

object TelephonyTagTest {

  /**
    * Old method from ru.yandex.realty.services.telepony.RedirectPhoneProcessor
    * @param o
    * @param keys
    * @return
    *
    * @see ru.yandex.realty.services.telepony.RedirectPhoneProcessor
    */
  private def getTag(o: Offer, keys: Set[String]): Option[String] = {

    val pairs = List(
      "certified" -> "no",
      "type" -> o.getOfferType.name().toLowerCase(),
      "category" -> o.getCategoryType.name().toLowerCase(),
      "flattype" -> o.getApartmentInfo.getFlatType.name().toLowerCase(),
      "owner" -> (if (o.getSaleAgent.getCategory == SalesAgentCategory.OWNER) "yes" else "no")
    ) ++
      getCommercialType(o).map("commercialtype" -> _) ++
      getVas(o).map("vas" -> _) ++
      getPeriod(o).map("period" -> _) :+
      ("offerid" -> o.getId)

    Some(pairs.filter(p => keys.contains(p._1)).map(p => p._1 + "=" + p._2).mkString("#"))
  }

  private def getVas(o: Offer): Option[String] = {
    if (o.isPremium) Some("premium")
    else if (o.isRaised) Some("raise")
    else if (o.hasPromotion) Some("promotion")
    else None
  }

  private def getCommercialType(o: Offer): Option[String] = {
    if (o.getCategoryType == CategoryType.COMMERCIAL) {
      o.getCommercialInfo.getCommercialType.asScala.headOption.map(_.name().toLowerCase)
    } else {
      None
    }
  }

  private def getPeriod(o: Offer): Option[String] = {
    if (o.getOfferType == OfferType.RENT) {
      o.getTransaction.getPrice.getPeriod match {
        case PricingPeriod.PER_MONTH => None
        case v => Some(v.name().toLowerCase())
      }
    } else None
  }

  private def getTag(o: Offer): Option[String] = {
    if (o.isFromVos && o.getSaleAgent.getCategory == SalesAgentCategory.OWNER) {
      getTag(o, AllKeys)
    } else {
      var keys = Set.empty[String]

      if (getVas(o).isDefined) keys ++= Seq("vas")
      if (isMonthRentApartment(o)) keys ++= Seq("type", "category")
      if (Regions.SPB_AND_LEN_OBLAST == o.getLocation.getSubjectFederationId) {
        if (o.getCategoryType == CategoryType.APARTMENT && o.getOfferType == OfferType.SELL)
          keys ++= Seq("type", "category")
        if (o.getCategoryType == CategoryType.COMMERCIAL) keys ++= Seq("type", "category")
      }

      if (keys.isEmpty) None
      else getTag(o, keys)
    }
  }

  private def isMonthRentApartment(o: Offer): Boolean = {
    o.getOfferType == OfferType.RENT &&
    o.getCategoryType == CategoryType.APARTMENT &&
    o.getTransaction.getPrice.getPeriod == PricingPeriod.PER_MONTH
  }

  private val AllKeys =
    Set("certified", "type", "category", "flattype", "owner", "commercialtype", "vas", "period", "offerid")
}
