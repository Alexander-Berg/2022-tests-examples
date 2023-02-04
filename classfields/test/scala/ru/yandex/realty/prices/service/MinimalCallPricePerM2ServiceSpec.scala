package ru.yandex.realty.prices.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.sites._
import ru.yandex.realty.prices.dao.PricePerM2Dao
import ru.yandex.realty.prices.model.PricePerM2
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService}
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class MinimalCallPricePerM2ServiceSpec extends AsyncSpecBase {

  private val pricePerM2Dao = mock[PricePerM2Dao]
  private val sitesService = mock[SitesGroupingService]
  private val companiesProvider = mock[Provider[CompaniesStorage]]
  private val minimalCallPricePerM2Service =
    new MinimalCallPricePerM2Service(pricePerM2Dao, sitesService, companiesProvider)

  private val siteId = 1
  private val companyId = 2
  private val agregateFlatInfo0 = new AgregateFlatInfo(Rooms.STUDIO, SaleStatus.ON_SALE, 10, 20, 300000, 700000)
  private val agregateFlatInfo1 = new AgregateFlatInfo(Rooms._1, SaleStatus.ON_SALE, 20, 30, 2500000, 5000000)
  private val agregateFlatInfo2 = new AgregateFlatInfo(Rooms._2, SaleStatus.ON_SALE, 30, 50, 5500000, 9000000)
  private val agregateFlatInfo3 = new AgregateFlatInfo(Rooms._3, SaleStatus.ON_SALE, 50, 80, 10000000, 15000000)
  private val agregateFlatInfo4 = new AgregateFlatInfo(Rooms._4, SaleStatus.ON_SALE, 80, 120, 27000000, 40000000)

  private val partnerId1 = 12421412412412L
  private val partnerId2 = 12421412412433L

  private val price0 =
    PricePerM2(
      siteId = siteId,
      partnerId = partnerId1,
      rooms = 0,
      minArea = 11,
      maxArea = 16,
      minPrice = 400000,
      maxPrice = 700000
    )

  private val price1 =
    PricePerM2(
      siteId = siteId,
      partnerId = partnerId1,
      rooms = 1,
      minArea = 20,
      maxArea = 33,
      minPrice = 2000000,
      maxPrice = 4000000
    )

  private val price2 =
    PricePerM2(
      siteId = siteId,
      partnerId = partnerId1,
      rooms = 2,
      minArea = 35,
      maxArea = 50,
      minPrice = 4000000,
      maxPrice = 6000000
    )

  private val price3 =
    PricePerM2(
      siteId = siteId,
      partnerId = partnerId2,
      rooms = 3,
      minArea = 60,
      maxArea = 80,
      minPrice = 8000000,
      maxPrice = 10000000
    )

  private val price4 = PricePerM2(
    siteId = siteId,
    partnerId = partnerId1,
    rooms = 4,
    minArea = 90,
    maxArea = 150,
    minPrice = 40000000,
    maxPrice = 90000000
  )

  implicit private val traced: Traced = Traced.empty

  "PricePerM2MinimalCostService in getPriceData" when {
    "service can't calculate price per m2" should {

      "return None if site has not offers and info about apartment prices in verba" in {
        val site = buildSite()
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq.empty)
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe None
      }

      "return None if site has offers " +
        "but has not info about apartment prices in verba and PriceDataSource.VERBA" in {
        val site = buildSite()
        val company = buildCompany(PriceDataSource.VERBA, Seq(partnerId1))
        mockAll(site, company)
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe None
      }

      "return None if site has not offers " +
        "but has info about apartment prices in verba and PriceDataSource.FEED" in {
        val site = buildSite(Seq(agregateFlatInfo0))
        val company = buildCompany(PriceDataSource.FEED, Seq(partnerId1))
        mockAll(site, company, Seq.empty)
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe None
      }
    }

    "service can calculate price per m2 and price per m2 < minThreshold" should {
      "return new matrix result (calc by offers - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo0))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price0))
        val expected = Some(MinimalCallPricePerM2Data(250000, 300000, 4005681))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.FEED)" in {
        val site = buildSite(Seq(agregateFlatInfo0))
        val company = buildCompany(PriceDataSource.FEED, Seq(partnerId1))
        mockAll(site, company, Seq(price0))
        val expected = Some(MinimalCallPricePerM2Data(250000, 300000, 4005681))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo0))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq.empty)
        val expected = Some(MinimalCallPricePerM2Data(250000, 300000, 3250000))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.VERBA)" in {
        val site = buildSite(Seq(agregateFlatInfo0))
        val company = buildCompany(PriceDataSource.VERBA, Seq(partnerId1))
        mockAll(site, company)
        val expected = Some(MinimalCallPricePerM2Data(250000, 300000, 3250000))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }
    }

    "service can calculate price per m2 and minThreshold < price per m2 < maxThreshold" should {
      "return new matrix result (calc by offers - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1, partnerId2))
        mockAll(site, company, Seq(price1, price2, price3))
        val expected = Some(MinimalCallPricePerM2Data(500000, 600000, 11897186))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.FEED)" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3))
        val company = buildCompany(PriceDataSource.FEED, Seq(partnerId1, partnerId2))
        mockAll(site, company, Seq(price1, price2, price3))
        val expected = Some(MinimalCallPricePerM2Data(500000, 600000, 11897186))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1, partnerId2))
        mockAll(site, company, Seq.empty)
        val expected = Some(MinimalCallPricePerM2Data(800000, 960000, 17375000))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.VERBA)" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3))
        val company = buildCompany(PriceDataSource.VERBA, Seq(partnerId1, partnerId2))
        mockAll(site, company)
        val expected = Some(MinimalCallPricePerM2Data(800000, 960000, 17375000))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }
    }

    "service can calculate price per m2 and price per m2 > maxThreshold" should {
      "return new matrix result (calc by offers - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo4))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price4))
        val expected = Some(MinimalCallPricePerM2Data(1500000, 1800000, 52222222))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.FEED)" in {
        val site = buildSite(Seq(agregateFlatInfo4))
        val company = buildCompany(PriceDataSource.FEED, Seq(partnerId1))
        mockAll(site, company, Seq(price4))
        val expected = Some(MinimalCallPricePerM2Data(1500000, 1800000, 52222222))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo4))
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq.empty)
        val expected = Some(MinimalCallPricePerM2Data(1500000, 1800000, 33541668))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by verba - PriceDataSource.VERBA)" in {
        val site = buildSite(Seq(agregateFlatInfo4))
        val company = buildCompany(PriceDataSource.VERBA, Seq(partnerId1))
        mockAll(site, company)
        val expected = Some(MinimalCallPricePerM2Data(1500000, 1800000, 33541668))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }
    }

    "service can calculate price per m2 and region = Spb" should {
      "return new matrix result (calc by offers - PriceDataSource.DEFAULT)" in {
        val site = buildSite(Seq(agregateFlatInfo4), Regions.SPB_AND_LEN_OBLAST)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price4))
        val expected = Some(MinimalCallPricePerM2Data(1300000, 1560000, 52222222))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.DEFAULT) with medium price per m2" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3), Regions.SPB_AND_LEN_OBLAST)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1, partnerId2))
        mockAll(site, company, Seq(price1, price2, price3))
        val expected = Some(MinimalCallPricePerM2Data(500000, 600000, 11897186))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.DEFAULT) with low price per m2" in {
        val site = buildSite(Seq(agregateFlatInfo0), Regions.SPB_AND_LEN_OBLAST)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price0))
        val expected = Some(MinimalCallPricePerM2Data(250000, 300000, 4005681))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }
    }

    "service can calculate price per m2 and region = Krasnodar" should {
      "return new matrix result (calc by offers - PriceDataSource.DEFAULT) with high price per m2" in {
        val site = buildSite(Seq(agregateFlatInfo4), Regions.KRASNODARSKYJ_KRAI)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price4))
        val expected = Some(MinimalCallPricePerM2Data(500000, 600000, 52222222))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.DEFAULT) with medium price per m2" in {
        val site = buildSite(Seq(agregateFlatInfo1, agregateFlatInfo2, agregateFlatInfo3), Regions.KRASNODARSKYJ_KRAI)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1, partnerId2))
        mockAll(site, company, Seq(price1, price2, price3))
        val expected = Some(MinimalCallPricePerM2Data(200000, 240000, 11897186))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }

      "return new matrix result (calc by offers - PriceDataSource.DEFAULT) with low price per m2" in {
        val site = buildSite(Seq(agregateFlatInfo0), Regions.KRASNODARSKYJ_KRAI)
        val company = buildCompany(PriceDataSource.DEFAULT, Seq(partnerId1))
        mockAll(site, company, Seq(price0))
        val expected = Some(MinimalCallPricePerM2Data(150000, 180000, 4005681))
        minimalCallPricePerM2Service.getPriceData(site.getId, company.getId).futureValue shouldBe expected
      }
    }
  }

  private def buildSite(
    agregateFlatInfos: Seq[AgregateFlatInfo] = Seq.empty,
    regionId: Int = Regions.MSK_AND_MOS_OBLAST
  ): Site = {
    val site = new Site(siteId)
    site.setAgregateFlatInfos(agregateFlatInfos.asJava)
    val l = new Location()
    l.setSubjectFederation(regionId, 0)
    site.setLocation(l)
    site
  }

  private def buildCompany(priceDataSource: PriceDataSource, partnerIds: Seq[Long] = Seq.empty): Company = {
    val company = new Company(companyId)
    company.setPriceDataSource(priceDataSource)
    company.setSources(partnerIds.map(new PartnerSeller(_, "saleAgent")).asJava)
    company
  }

  private def mockAll(site: Site, company: Company): Unit = {
    (companiesProvider.get _).expects().returning(new CompaniesStorage(Seq(company).asJava))
    (sitesService.getSiteById _).expects(site.getId).returning(site)
  }

  private def mockAll(site: Site, company: Company, prices: Seq[PricePerM2]): Unit = {
    mockAll(site, company)
    val partnerIds: Set[Long] = company.getSources.asScala.map(_.getPartnerId).toSet
    (pricePerM2Dao
      .get(_: Long, _: Set[Long])(_: Traced))
      .expects(site.getId, partnerIds, *)
      .returning(
        Future(prices)
      )
  }
}
