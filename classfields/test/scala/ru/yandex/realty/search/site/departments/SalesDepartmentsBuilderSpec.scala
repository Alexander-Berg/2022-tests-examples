package ru.yandex.realty.search.site.departments

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.gen.location.LocationGenerator
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.model.sites.{Company, Site}
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.search.site.callcenter.{CallCenterPhonesService, DefaultCallCenterRegionsService}
import ru.yandex.realty.sites.auction.AuctionStepService
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{CampaignDumpProvider, CompaniesStorage, ExtendedSiteStatisticsStorage}

import java.util.Collections.{emptyList, emptyMap, singletonList}
import java.util.Optional
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SalesDepartmentsBuilderSpec
  extends SpecBase
  with OneInstancePerTest
  with LocationGenerator
  with RegionGraphTestComponents {

  private val idGen: Gen[Long] = Gen.posNum[Long]

  private val phoneGen: Gen[String] = for {
    country <- Gen.choose(0, 9)
    code <- Gen.choose(0, 999)
    phone <- Gen.choose(0, 9999999)
  } yield f"$country%01d-$code%03d-$phone%07d"

  private val siteLocation: Location = locationGen(Regions.KAZAN).next

  private val campaignProvider = mock[Provider[CampaignStorage]]
  private val companiesProvider = mock[Provider[CompaniesStorage]]
  private val extendedSiteStatisticsStorageProvider = mock[Provider[ExtendedSiteStatisticsStorage]]
  private val callCenterRegionsService = new DefaultCallCenterRegionsService(
    Set(siteLocation.getGeocoderId.intValue()),
    Set.empty,
    regionGraphProvider
  )
  private val callCenterPhonesService = new CallCenterPhonesService(
    Map(siteLocation.getGeocoderId.intValue() -> generatePhones()),
    callCenterRegionsService
  )
  private val auctionStepService = mock[AuctionStepService]
  private val campaignDumpProvider = new CampaignDumpProvider(auctionStepService)
  private val salesDepartmentsBuilder =
    new SalesDepartmentsBuilder(
      campaignProvider,
      companiesProvider,
      extendedSiteStatisticsStorageProvider,
      campaignDumpProvider,
      callCenterRegionsService
    )

  (auctionStepService.getStepForSite _).expects(*).anyNumberOfTimes().returning(10000)

  "SalesDepartmentsBuilder" should {

    "create sales department from company with site redirect phones" in {
      val site = generateSite {
        SiteConfig(
          withPhones = true,
          withRedirectPhones = true,
          hasExclusiveSeller = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual company.getId
      salesDepartments.getName shouldEqual company.getName
      val phones = site.getSalesDepartmentRedirects(Optional.empty[String]).asScala.map(_.getSource)
      salesDepartments.getPhonesList.asScala shouldEqual phones
      salesDepartments.getPhonesWithTagList.asScala.map(_.getPhone) shouldEqual phones
      salesDepartments.getIsRedirectPhones shouldBe true
    }

    "create sales department from company with site target phones" in {
      val site = generateSite {
        SiteConfig(
          withPhones = true,
          hasExclusiveSeller = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual company.getId
      salesDepartments.getName shouldEqual company.getName
      salesDepartments.getPhonesList.asScala shouldEqual site.getSalesDepartmentPhones.asScala
      salesDepartments.getIsRedirectPhones shouldBe false
    }

    "create sales department from company with company redirect phones" in {
      val site = generateSite {
        SiteConfig(
          hasExclusiveSeller = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual company.getId
      salesDepartments.getName shouldEqual company.getName
      salesDepartments.getPhonesList.asScala shouldEqual company.getRedirectPhoneNumbers.asScala
      salesDepartments.getPhonesWithTagList.asScala.map(_.getPhone) shouldEqual company.getRedirectPhoneNumbers.asScala
      salesDepartments.getIsRedirectPhones shouldBe true
    }

    "create sales department from company with company phones" in {
      val site = generateSite {
        SiteConfig(
          hasExclusiveSeller = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual company.getId
      salesDepartments.getName shouldEqual company.getName
      salesDepartments.getPhonesList.asScala shouldEqual company.getPhoneNumbers.asScala
      salesDepartments.getIsRedirectPhones shouldBe false
    }

    "create sales department from builder with site redirect phones" in {
      val site = generateSite {
        SiteConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName
      val phones = site.getSalesDepartmentRedirects(Optional.empty[String]).asScala.map(_.getSource)
      salesDepartments.getPhonesList.asScala shouldEqual phones
      salesDepartments.getPhonesWithTagList.asScala.map(_.getPhone) shouldEqual phones
      salesDepartments.getIsRedirectPhones shouldBe true
    }

    "create sales department from builder with site target phones" in {
      val site = generateSite {
        SiteConfig(
          withPhones = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName
      salesDepartments.getPhonesList.asScala shouldEqual site.getSalesDepartmentPhones.asScala
      salesDepartments.getIsRedirectPhones shouldBe false
    }

    "create sales department from builder with builder redirect phones" in {
      val site = generateSite {
        SiteConfig()
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName
      salesDepartments.getPhonesList.asScala shouldEqual builder.getRedirectPhoneNumbers.asScala
      salesDepartments.getPhonesWithTagList.asScala.map(_.getPhone) shouldEqual builder.getRedirectPhoneNumbers.asScala
      salesDepartments.getIsRedirectPhones shouldBe true
    }

    "create sales department from builder with builder phones" in {
      val site = generateSite {
        SiteConfig()
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName
      salesDepartments.getPhonesList.asScala shouldEqual builder.getPhoneNumbers.asScala
      salesDepartments.getIsRedirectPhones shouldBe false
    }

    "create sales department from builder with builder phones redirected to call center" in {
      val site = generateSite {
        SiteConfig(
          isCallCenterPhonesUsed = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true,
          withCallCenterRedirectPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName

      val siteRegionId = site.getLocation.getGeocoderId
      salesDepartments.getPhonesList.asScala shouldEqual builder
        .getCallCenterRedirectPhones(siteRegionId)
        .asScala
        .map(_.getSource)
      salesDepartments.getPhonesWithTagList.asScala
        .map(_.getPhone) shouldEqual builder.getCallCenterRedirectPhones(siteRegionId).asScala.map(_.getSource)
      salesDepartments.getIsRedirectPhones shouldBe true
    }

    "create sales department from builder with call center phones" in {
      val site = generateSite {
        SiteConfig(
          isCallCenterPhonesUsed = true
        )
      }

      val company = generateCompany(site) {
        CompanyConfig(
          withPhones = true,
          withRedirectPhones = true
        )
      }

      val builder = generateBuilder(site) {
        CompanyConfig(
          withPhones = true
        )
      }

      mockStorages(
        companies = new CompaniesStorage(Seq(company, builder).asJava)
      )

      val salesDepartments = salesDepartmentsBuilder.buildTopDepartment(site)

      salesDepartments.getId shouldEqual builder.getId
      salesDepartments.getName shouldEqual builder.getName

      val siteRegionId = site.getLocation.getGeocoderId
      salesDepartments.getPhonesList.asScala shouldEqual callCenterPhonesService
        .getCallCenterPhones(siteRegionId)
        .asScala
      salesDepartments.getIsRedirectPhones shouldBe false
    }

    def generateSite(config: => SiteConfig): Site = {
      val site = new Site(idGen.next)
      site.setLocation(siteLocation)
      site.setCallCenterPhonesCanBeUsed(config.isCallCenterPhonesUsed)
      site.setExclusiveSeller(config.hasExclusiveSeller)

      if (config.withPhones) {
        site.setSalesDepartmentPhones(
          generatePhones().asJava
        )
        if (config.isCallCenterPhonesUsed) {
          val callCenterPhones = callCenterPhonesService.getCallCenterPhones(site.getLocation.getGeocoderId)
          site.setSalesDepartmentCallCenterPhones(callCenterPhones)
        }
      }
      if (config.withRedirectPhones) {
        site.setSalesDepartmentRedirects(generateRedirectPhones().asJava)
      }

      site
    }

    def generateCompany(site: Site)(config: => CompanyConfig): Company = {
      val company = new Company(idGen.next)
      company.setName("Company #" + company.getId)
      company.setBuildings(singletonList(site.getId))

      val siteRegionId = Int.box(site.getLocation.getGeocoderId)

      if (config.withPhones) {
        company.setPhoneNumbers(generatePhones().asJava)
      }
      if (config.withRedirectPhones) {
        company.setRedirectPhones(generateRedirectPhones().asJava)
      }
      if (config.withCallCenterRedirectPhones) {
        company.setCallCenterPhoneNumbers(
          Map(siteRegionId -> callCenterPhonesService.getCallCenterPhones(siteRegionId)).asJava
        )
        company.setCallCenterRedirectPhones(Map(siteRegionId -> generateRedirectPhones().asJava).asJava)
      }

      company
    }

    def generateBuilder(site: Site)(config: => CompanyConfig): Company = {
      val builder = new Company(idGen.next)
      builder.setName("Builder #" + builder.getId)

      val siteRegionId = Int.box(site.getLocation.getGeocoderId)

      if (config.withPhones) {
        builder.setPhoneNumbers(generatePhones().asJava)
        builder.setCallCenterPhoneNumbers(
          Map(siteRegionId -> callCenterPhonesService.getCallCenterPhones(siteRegionId)).asJava
        )
      }
      if (config.withRedirectPhones) {
        builder.setRedirectPhones(generateRedirectPhones().asJava)
      }
      if (config.withCallCenterRedirectPhones) {
        builder.setCallCenterRedirectPhones(Map(siteRegionId -> generateRedirectPhones().asJava).asJava)
      }

      site.setBuilders(singletonList(builder.getId))

      builder
    }

    def mockStorages(
      companies: CompaniesStorage = new CompaniesStorage(emptyList()),
      campaignStorage: CampaignStorage = new CampaignStorage(emptyList()),
      extendedSiteStatisticsStorage: ExtendedSiteStatisticsStorage = new ExtendedSiteStatisticsStorage(emptyMap())
    ): Unit = {
      mockCompanies {
        companies
      }

      mockCampaigns {
        campaignStorage
      }

      mockSiteStatistics {
        extendedSiteStatisticsStorage
      }
    }

    def mockCompanies(storage: => CompaniesStorage): Unit = {
      (companiesProvider.get _).expects().anyNumberOfTimes().returning(storage)
    }

    def mockCampaigns(storage: => CampaignStorage): Unit = {
      (campaignProvider.get _).expects().anyNumberOfTimes().returning(storage)
    }

    def mockSiteStatistics(storage: => ExtendedSiteStatisticsStorage): Unit = {
      (extendedSiteStatisticsStorageProvider.get _).expects().anyNumberOfTimes().returning(storage)
    }

  }

  def generatePhones(): Seq[String] =
    phoneGen.next(Gen.chooseNum(1, 4).next).toSeq

  def generateRedirectPhones(): Seq[PhoneRedirectMessage] =
    generatePhones()
      .map(
        PhoneRedirectMessage
          .newBuilder()
          .setSource(_)
          .build()
      )

  case class SiteConfig(
    withPhones: Boolean = false,
    withRedirectPhones: Boolean = false,
    isCallCenterPhonesUsed: Boolean = false,
    hasExclusiveSeller: Boolean = false
  )

  case class CompanyConfig(
    withPhones: Boolean = false,
    withRedirectPhones: Boolean = false,
    withCallCenterRedirectPhones: Boolean = false
  )

}
