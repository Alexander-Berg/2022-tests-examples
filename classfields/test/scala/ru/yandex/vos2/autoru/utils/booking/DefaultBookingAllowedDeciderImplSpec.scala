package ru.yandex.vos2.autoru.utils.booking

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.mockito.MockitoSupport.{mock, when}
import ru.yandex.vos2.autoru.model.extdata.DefaultBookingAllowedDictionary
import ru.yandex.vos2.autoru.model.extdata.DefaultBookingAllowedDictionary.ByRegion
import ru.yandex.vos2.autoru.utils.booking.impl.DefaultBookingAllowedDeciderImpl
import ru.yandex.vos2.autoru.utils.geo.GeoIds.GeoId
import ru.yandex.vos2.autoru.utils.geo.Tree
import ru.yandex.vos2.model.{UserRef, UserRefAutoru, UserRefAutoruClient}

@RunWith(classOf[JUnitRunner])
class DefaultBookingAllowedDeciderImplSpec extends AnyWordSpec {

  private val dictionary = DefaultBookingAllowedDictionary(
    byRegions = Set(
      ByRegion(Category.CARS, Section.NEW, geobaseIds = Set(1L, 2L))
    ),
    excludedDealerIds = Set(UserRefAutoruClient(1), UserRefAutoruClient(2))
  )

  private val geoTree: Tree = mock[Tree]
  when(geoTree.isInside(100L, Set(1L, 2L))).thenReturn(true)
  when(geoTree.isInside(200L, Set(1L, 2L))).thenReturn(false)

  private val decider = new DefaultBookingAllowedDeciderImpl(dictionary, geoTree)

  private case class TestCase(description: String,
                              userRef: UserRef,
                              category: Category,
                              section: Section,
                              geobaseId: GeoId,
                              expected: Boolean)

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      "dealer with default allowed",
      UserRefAutoruClient(3),
      Category.CARS,
      Section.NEW,
      geobaseId = 100L,
      expected = true
    ),
    TestCase(
      "dealer with default disallowed",
      UserRefAutoruClient(3),
      Category.CARS,
      Section.NEW,
      geobaseId = 200L,
      expected = false
    ),
    TestCase(
      "excluded dealer with default disallowed",
      UserRefAutoruClient(2),
      Category.CARS,
      Section.NEW,
      geobaseId = 100L,
      expected = false
    ),
    TestCase(
      "private user with default disallowed",
      UserRefAutoru(3),
      Category.CARS,
      Section.NEW,
      geobaseId = 100L,
      expected = false
    )
  )

  "DefaultBookingAllowedDeciderImpl.decide" when {
    testCases.foreach {
      case TestCase(description, userRef, category, section, geobaseId, expected) =>
        description in {
          val actual = decider.decide(userRef, category, section, geobaseId)
          assert(actual == expected)
        }
    }
  }
}
