package ru.yandex.realty2.extdataloader.loaders.sites

import java.lang.{Boolean => JBoolean}
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.message.ExtDataSchema.SiteApartmentTypeNamespace.SiteApartmentType

@RunWith(classOf[JUnitRunner])
class SiteApartmentTypeResolverSpec extends WordSpec with Matchers with MockFactory {

  private val testCases = Seq(
    TestCase(
      isApartment = null,
      isMixed = null,
      expectedSiteApartmentType = SiteApartmentType.UNKNOWN
    ),
    TestCase(
      isApartment = null,
      isMixed = true,
      expectedSiteApartmentType = SiteApartmentType.UNKNOWN
    ),
    TestCase(
      isApartment = false,
      isMixed = false,
      expectedSiteApartmentType = SiteApartmentType.FLATS
    ),
    TestCase(
      isApartment = false,
      isMixed = true,
      expectedSiteApartmentType = SiteApartmentType.FLATS
    ),
    TestCase(
      isApartment = true,
      isMixed = null,
      expectedSiteApartmentType = SiteApartmentType.APARTMENTS
    ),
    TestCase(
      isApartment = true,
      isMixed = false,
      expectedSiteApartmentType = SiteApartmentType.APARTMENTS
    ),
    TestCase(
      isApartment = true,
      isMixed = true,
      expectedSiteApartmentType = SiteApartmentType.APARTMENTS_AND_FLATS
    )
  )

  private val siteApartmentTypeResolver = new SiteApartmentTypeResolver

  "SiteApartmentTypeResolver" should {

    testCases.zipWithIndex.foreach {
      case (TestCase(isApartment, isMixed, expectedSiteApartmentType), i) =>
        s"resolve apartment type correctly #$i" in {
          val actualApartmentType = siteApartmentTypeResolver.resolveApartmentType(isApartment, isMixed)
          actualApartmentType should be(expectedSiteApartmentType)
        }
    }

  }

  private case class TestCase(isApartment: JBoolean, isMixed: JBoolean, expectedSiteApartmentType: SiteApartmentType)

}
