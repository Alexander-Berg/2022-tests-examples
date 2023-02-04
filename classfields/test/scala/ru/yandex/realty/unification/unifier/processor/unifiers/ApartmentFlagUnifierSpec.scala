package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.generator.RawOfferGenerator
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.message.ExtDataSchema.SiteApartmentTypeNamespace.SiteApartmentType
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.vertis.generators.ProducerProvider._

@RunWith(classOf[JUnitRunner])
class ApartmentFlagUnifierSpec extends AsyncSpecBase with MockFactory with Matchers with OneInstancePerTest {

  private val sitesService = mock[SitesGroupingService]

  private val unifier = new ApartmentFlagUnifier(sitesService)

  private val rawOfferFlagCases: Seq[Option[Boolean]] = Seq(
    Some(true),
    Some(false),
    None
  )

  private val siteApartmentTypeToApartmentFlagCases: Seq[(SiteApartmentType, java.lang.Boolean)] = Seq(
    (SiteApartmentType.APARTMENTS_AND_FLATS, false),
    (SiteApartmentType.APARTMENTS, true),
    (SiteApartmentType.FLATS, false),
    (SiteApartmentType.UNKNOWN, null)
  )

  implicit val trace: Traced = Traced.empty

  "ApartmentFlagUnifier" should {

    rawOfferFlagCases.foreach { rawOfferFlag =>
      s"set apartments flag to $rawOfferFlag from raw offer when site is not found" in {
        val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = rawOfferFlag).next
        val offer = OfferModelGenerators
          .offerGen(
            withApartmentsFlag = false
          )
          .next

        unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

        offer.getApartmentInfo.getApartments shouldBe (rawOfferFlag.map(Boolean.box).orNull)
      }

      s"set apartments flag to $rawOfferFlag from raw offer when site has ${SiteApartmentType.UNKNOWN} type" in {
        val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = rawOfferFlag).next
        val offer = OfferModelGenerators
          .offerGen(
            withApartmentsFlag = false,
            withBuildingInfo = true
          )
          .next

        val site = new Site(1)
        site.setApartmentType(SiteApartmentType.UNKNOWN)
        (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

        unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

        offer.getApartmentInfo.getApartments shouldBe rawOfferFlag.map(Boolean.box).orNull
      }

      s"set apartments flag from site info and ignore raw offer $rawOfferFlag value when site has ${SiteApartmentType.APARTMENTS} type" in {
        val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = rawOfferFlag).next
        val offer = OfferModelGenerators
          .offerGen(
            withBuildingInfo = true,
            withApartmentsFlag = false
          )
          .next

        val site = new Site(1)
        site.setApartmentType(SiteApartmentType.APARTMENTS)
        (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

        unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

        offer.getApartmentInfo.getApartments shouldBe true
      }

      s"set apartments flag from raw site info and ignore raw offer $rawOfferFlag value when site has ${SiteApartmentType.FLATS} type" in {
        val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = rawOfferFlag).next
        val offer = OfferModelGenerators
          .offerGen(
            withBuildingInfo = true,
            withApartmentsFlag = false
          )
          .next

        val site = new Site(1)
        site.setApartmentType(SiteApartmentType.FLATS)
        (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

        unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

        offer.getApartmentInfo.getApartments shouldBe false
      }

    }

    siteApartmentTypeToApartmentFlagCases.foreach {
      case (siteApartmentType, expectedApartmentFlag) =>
        s"set apartments flag to $expectedApartmentFlag from site with $siteApartmentType apartment type when apartment flag is not defined in raw offer" in {
          val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = None).next
          val offer = OfferModelGenerators
            .offerGen(
              withBuildingInfo = true,
              withApartmentsFlag = false
            )
            .next

          val site = new Site(1)
          site.setApartmentType(siteApartmentType)
          (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

          unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

          offer.getApartmentInfo.getApartments shouldBe expectedApartmentFlag
        }
    }

    Seq(true, false).foreach { rawOfferFlag =>
      val siteApartmentType = SiteApartmentType.APARTMENTS_AND_FLATS

      s"set apartments flag from raw offer when site has $siteApartmentType type and raw offer apartment type is defined to $rawOfferFlag" in {
        val rawOffer = RawOfferGenerator.rawOfferGen(withApartments = Some(rawOfferFlag)).next
        val offer = OfferModelGenerators
          .offerGen(
            withBuildingInfo = true,
            withApartmentsFlag = false
          )
          .next

        val site = new Site(1)
        site.setApartmentType(siteApartmentType)
        (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(site)

        unifier.unify(new OfferWrapper(rawOffer, offer, null)).futureValue

        offer.getApartmentInfo.getApartments shouldBe rawOfferFlag
      }
    }

  }

}
