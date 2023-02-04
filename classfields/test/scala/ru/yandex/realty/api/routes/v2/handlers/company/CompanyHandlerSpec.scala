package ru.yandex.realty.api.routes.v2.handlers.company

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.ProtoResponse.CompanyObjectsResponse
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v2.company.CompanyHandlerImpl
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.company.CompanyConverters._
import ru.yandex.realty.managers.company.CompanyManager
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.languageFeature.postfixOps

@RunWith(classOf[JUnitRunner])
class CompanyHandlerSpec
  extends HandlerSpecBase
  with PropertyChecks
  with UserRefGenerators
  with BasicDirectives
  with ScalatestRouteTest
  with SearcherResponseModelGenerators {

  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler

  implicit val excHandler: ExceptionHandler = exceptionHandler
  implicit val rejHandler: RejectionHandler = rejectionHandler

  override def routeUnderTest: Route = Route.seal(new CompanyHandlerImpl(manager).route)

  private val manager: CompanyManager = mock[CompanyManager]
  private val notFound = new NoSuchElementException()

  private val mockGetObjects =
    toMockFunction2(manager.getObjects(_: Long)(_: Traced))

  "companyHandler" when {
    "getObjects" should {
      "propagate missing companyId as NOT_FOUND" in {
        inSequence {
          forAll(companyIdGen) { companyId =>
            mockGetObjects
              .expects(companyId, *)
              .anyNumberOfTimes()
              .throwingF(notFound)

            Get(
              Uri(s"/company/$companyId/objects")
            ).acceptingProto ~>
              route ~>
              check {
                status should be(StatusCodes.NotFound)
              }
          }
        }
      }

      "propagate InternalServerError for badly-formed responses (invalid CompanyObjects id)" in {
        inSequence {
          forAll(companyIdGen, companyObjectsInvalidIdGen) { (companyId, companyObjects) =>
            val result = Future { toResponse(companyObjects) }

            mockGetObjects
              .expects(companyId, *)
              .anyNumberOfTimes()
              .returning(result)

            Get(
              Uri(s"/company/$companyId/objects")
            ).acceptingProto ~>
              route ~>
              check {
                status should be(StatusCodes.InternalServerError)
              }
          }
        }
      }

      "propagate InternalServerError for badly-formed responses (CompanyBuilding with invalid Id)" in {
        inSequence {
          forAll(companyIdGen, companyObjectsGen, companyBuildingInvalidIdGen) {
            (companyId, companyObjects, badBuilding) =>
              val invalidCompanyBuildingInjected = companyObjects.copy(buildings = Some(Seq(badBuilding)))
              val result = Future { toResponse(invalidCompanyBuildingInjected) }

              mockGetObjects
                .expects(companyId, *)
                .anyNumberOfTimes()
                .returning(result)

              Get(
                Uri(s"/company/$companyId/objects")
              ).acceptingProto ~>
                route ~>
                check {
                  status should be(StatusCodes.InternalServerError)
                }
          }
        }
      }

      "propagate InternalServerError for badly-formed responses (CompanyVillage with invalid Id)" in {
        inSequence {
          forAll(companyIdGen, companyObjectsInvalidIdGen, companyVillageInvalidIdGen) {
            (companyId, companyObjects, badVillage) =>
              val invalidCompanyVillageInjected = companyObjects.copy(villages = Some(Seq(badVillage)))
              val result = Future { toResponse(invalidCompanyVillageInjected) }

              mockGetObjects
                .expects(companyId, *)
                .anyNumberOfTimes()
                .returning(result)

              Get(
                Uri(s"/company/$companyId/objects")
              ).acceptingProto ~>
                route ~>
                check {
                  status should be(StatusCodes.InternalServerError)
                }
          }
        }
      }

      "conclude with some valid response for a valid company" in {
        inSequence {
          forAll(companyIdGen, companyObjectsGen) { (companyId, companyObjects) =>
            val result = toResponse(companyObjects)

            mockGetObjects
              .expects(companyId, *)
              .anyNumberOfTimes()
              .returningF(result)

            Get(
              Uri(s"/company/$companyId/objects")
            ).acceptingProto ~>
              route ~>
              check {
                status should be(StatusCodes.OK)
                entityAs[CompanyObjectsResponse] should be(result)
              }
          }
        }
      }
    }
  }

}
