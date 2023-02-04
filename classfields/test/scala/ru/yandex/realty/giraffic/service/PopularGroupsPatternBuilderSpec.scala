package ru.yandex.realty.giraffic.service

import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.params.RequestParameter.HasAgentFee
import ru.yandex.realty.canonical.base.params.{Parameter, ParameterType, RequestParameter}
import ru.yandex.realty.canonical.base.request.{Request, RequestType}
import ru.yandex.realty.giraffic.model.links.LinkPattern
import ru.yandex.realty.giraffic.service.impl.patternBuilders.PopularGroupsPatternBuilder
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import zio._
import zio.random.Random
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.test.Assertion._

@RunWith(classOf[ZTestJUnitRunner])
class PopularGroupsPatternBuilderSpec extends JUnitRunnableSpec {
  private def getLinks(request: Request): Task[Iterable[LinkPattern]] =
    PopularGroupsPatternBuilder
      .buildGroupsPattern(request)
      .map { _.groups.flatMap(_.linksPattern.links) }

  private def hasParameterType(pt: ParameterType.Value): Assertion[LinkPattern] =
    assertion("hasParameter")(Render.param(pt)) { linkPattern =>
      linkPattern.linkRequest.params.exists(_.`type` == pt)
    }

  private val noiseParams: Set[Parameter] = Set(HasAgentFee(false))

  private def requestGen(categories: Set[CategoryType]): Gen[Random, Request] = {
    for {
      tp <- Gen.fromIterable(OfferType.values().filter(_ != OfferType.UNKNOWN))
      ct <- Gen.fromIterable(categories)
      noise <- Gen.fromIterable(noiseParams.map(Seq(_)) ++ Iterable(Seq.empty))
    } yield Request.Raw(
      RequestType.Search,
      Seq(
        RequestParameter.Type(tp),
        RequestParameter.Category(ct),
        RequestParameter.Rgid(1L)
      ) ++ noise
    )
  }

  private val nonApartmentRequestGen =
    requestGen(CategoryType.values().toSet.diff(Set(CategoryType.UNKNOWN, CategoryType.UNUSED, CategoryType.APARTMENT)))

  private val apartmentRequestGen =
    requestGen(Set(CategoryType.APARTMENT))

  private val houseRequestGen =
    requestGen(Set(CategoryType.HOUSE))

  private val commercialRequestGen =
    requestGen(Set(CategoryType.COMMERCIAL))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PopularGroupsPatternBuilder")(
      testM("should return newFlat and minFloor for apartment category") {
        checkAllM(apartmentRequestGen) { req =>
          getLinks(req).map { links =>
            assert(links)(exists(hasParameterType(ParameterType.NewFlat))) &&
            assert(links)(exists(hasParameterType(ParameterType.FloorMin)))
          }
        }
      },
      testM("should not return newFlat and minFloor for non apartment category") {
        checkAllM(nonApartmentRequestGen) { req =>
          getLinks(req).map { links =>
            assert(links)(not(exists(hasParameterType(ParameterType.NewFlat)))) &&
            assert(links)(not(exists(hasParameterType(ParameterType.FloorMin))))
          }
        }
      },
      testM("should return commercialType for commercial category") {
        checkAllM(commercialRequestGen) { req =>
          getLinks(req).map { links =>
            assert(links)(exists(hasParameterType(ParameterType.CommercialType)))
          }
        }
      },
      testM("should not return rooms for houses") {
        checkAllM(houseRequestGen) { req =>
          getLinks(req).map { links =>
            assert(links)(not(exists(hasParameterType(ParameterType.RoomsTotal))))
          }
        }
      },
      testM("should return all five room variations for apartment") {
        checkAllM(apartmentRequestGen) { req =>
          getLinks(req).map { links =>
            val res =
              links.filter(l => l.linkRequest.params.exists(_.`type` == ParameterType.RoomsTotal))
            assertTrue(res.size % 5 == 0)
          }
        }
      }
    )
}
