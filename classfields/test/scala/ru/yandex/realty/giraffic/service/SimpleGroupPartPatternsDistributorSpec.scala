package ru.yandex.realty.giraffic.service

import org.junit.runner.RunWith
import org.scalatest.enablers.Emptiness
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.canonical.base.params.RequestParameter._
import ru.yandex.realty.canonical.base.request.Request
import ru.yandex.realty.giraffic.model.links.{
  GroupPartPattern,
  GroupPatterns,
  LinkPattern,
  LinkSelectionStrategy,
  LinksPattern
}
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.giraffic.utils.SimpleGroupsPatternDistributor
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration

@RunWith(classOf[JUnitRunner])
class SimpleGroupPartPatternsDistributorSpec extends WordSpec with Matchers {

  import CheckUtils._

  private val FilterFurniture = FilterDeclaration.RawFilter(
    FilterDeclaration.wrapName("with-furniture"),
    Set(HasFurniture(true))
  )

  private val FilterMetroGeoId2 = FilterDeclaration.RawFilter(
    FilterDeclaration.wrapName("metro-2"),
    Set(MetroGeoId(2))
  )

  implicit private val emptiness: Emptiness[GroupPatterns] =
    (thing: GroupPatterns) => thing.groups.isEmpty

  "SimpleGroupsBuilder" should {

    "return empty groups when no filters" in {
      SimpleGroupsPatternDistributor.distributeFiltersToPatterns(
        TestData.MoscowSellApartmentReq,
        Seq.empty,
        Map.empty,
        Map.empty
      ) shouldBe empty
    }

    "return empty groups when has filters and no groups" in {
      SimpleGroupsPatternDistributor.distributeFiltersToPatterns(
        TestData.MoscowSellApartmentReq,
        Seq(FilterFurniture, FilterMetroGeoId2),
        Map.empty,
        Map(
          FilterFurniture.name -> "furniture text",
          FilterMetroGeoId2.name -> "metro-2 text"
        )
      ) shouldBe empty
    }

    "not use filter" when {

      "it has common param with request" in {
        SimpleGroupsPatternDistributor.distributeFiltersToPatterns(
          TestData.MoscowSellApartmentWithFurnitureReq,
          Seq(FilterFurniture),
          Map("g1" -> Set(FilterFurniture.name)),
          Map(FilterFurniture.name -> "furniture text")
        ) shouldBe empty
      }

      "it has inconsistent classified with request" in {

        val filterInconsistent = FilterDeclaration.RawFilter(
          FilterDeclaration.wrapName("2-rooms-with-furniture"),
          Set(RoomsTotal(RoomsValue.TwoRooms), HasFurniture(true))
        )

        val request =
          TestData.MoscowSellApartmentReq.addParams(RoomsTotal(RoomsValue.OneRoom))

        SimpleGroupsPatternDistributor
          .distributeFiltersToPatterns(
            request,
            Seq(filterInconsistent),
            Map("g1" -> Set(filterInconsistent.name)),
            Map(
              filterInconsistent.name -> "двушки с мебелью"
            )
          ) shouldBe empty
      }
    }

    def distributeSpec(inputRequest: Request) = {

      val groups = SimpleGroupsPatternDistributor
        .distributeFiltersToPatterns(
          inputRequest,
          Seq(FilterFurniture, FilterMetroGeoId2),
          Map(
            "g1" -> Set(FilterFurniture.name),
            "g2" -> Set(
              FilterMetroGeoId2.name,
              FilterFurniture.name
            )
          ),
          Map(
            FilterMetroGeoId2.name -> "у метро X",
            FilterFurniture.name -> "с мебелью"
          )
        )

      val expected = GroupPatterns(
        Iterable(
          GroupPartPattern(
            "g1",
            LinksPattern(
              Iterable(
                LinkPattern(
                  inputRequest.addFilterParams(FilterFurniture),
                  "с мебелью"
                )
              ),
              LinkSelectionStrategy.TakeAllWithOffers
            )
          ),
          GroupPartPattern(
            "g2",
            LinksPattern(
              Iterable(
                LinkPattern(
                  inputRequest.addFilterParams(FilterFurniture),
                  "с мебелью"
                ),
                LinkPattern(
                  inputRequest.addFilterParams(FilterMetroGeoId2),
                  "у метро X"
                )
              ),
              LinkSelectionStrategy.TakeAllWithOffers
            )
          )
        )
      )

      checkSameGroupsPattern(expected, groups) shouldBe true
    }

    "correctly distribute" in {
      for {
        request <- Seq(
          TestData.MoscowSellApartmentReq,
          TestData.MoscowSellApartmentReq.addParams(SiteId(1), SiteName("SITE_NAME"))
        )
      } withClue(s"on $request") {
        distributeSpec(request)
      }
    }
  }
}
