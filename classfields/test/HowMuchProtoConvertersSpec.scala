package vsmoney.auction.converters.test

import billing.common_model.{Money, Project => ProtoProject}
import billing.howmuch.model.Source.Source.{ServiceRequest, UserRequest}
import billing.howmuch.model.{RequestContext, RequestCriteria, RuleContext, RuleCriteria, Source}
import billing.howmuch.price_service.{
  GetPricesRequest,
  GetPricesRequestEntry,
  GetPricesResponse,
  GetPricesResponseEntry,
  PatchPricesRequest,
  PatchPricesRequestEntry
}
import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf
import vsmoney.auction.model._
import vsmoney.auction.model.howmuch.{
  ChangePriceRequest,
  PriceRequest,
  PriceRequestEntry,
  PriceResponse,
  PriceResponseEntry
}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object HowMuchProtoConvertersSpec extends DefaultRunnableSpec {
  import vsmoney.auction.converters.HowMuchProtoConverters._

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("HowMuchProtoConverters")(
      suite("main converters")(
        testM("should convert PriceRequest to GetPricesRequest") {
          val testDate = Instant.now()
          val testRequest = PriceRequest(
            Project.Autoru,
            testDate,
            List(
              PriceRequestEntry(
                entryId,
                MatrixId.ProductMatrixId(ProductId(product)),
                CriteriaContext(List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value))))
              )
            )
          )

          val testProtoRequest = GetPricesRequest(
            ProtoProject.AUTORU,
            List(
              GetPricesRequestEntry(
                entryId,
                product,
                Some(RequestContext(List(RequestCriteria(criterion1key, criterion1value))))
              )
            ),
            Some(ScalaProtobuf.instantToTimestamp(testDate))
          )
          assertM(getRequestToProto.convert(testRequest))(equalTo(testProtoRequest))
        },
        testM("should convert GetPricesResponse to PriceResponse") {
          val testPriceSource = Source(ServiceRequest("promo-campaign-1"))

          val testGetPricesResponse = GetPricesResponse(
            List(
              GetPricesResponseEntry(
                entryId,
                GetPricesResponseEntry.Result.Rule(
                  GetPricesResponseEntry.Rule(
                    testRule,
                    Some(Money(testPrice)),
                    Some(testPriceSource)
                  )
                )
              )
            )
          )

          val testPriceResponse = PriceResponse(
            List(
              PriceResponseEntry(
                entryId,
                testRule,
                Kopecks(testPrice),
                testPriceSource
              )
            )
          )
          assertM(getResponseFromProto.convert(testGetPricesResponse))(equalTo(testPriceResponse))
        },
        testM("should convert ChangePriceRequest to PatchPricesRequest") {
          val testRequest = ChangePriceRequest(
            Project.Autoru,
            MatrixId.ProductMatrixId(ProductId(product)),
            CriteriaContext(List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))),
            Some(Kopecks(prevPrice)),
            Some(Kopecks(testPrice)),
            Some(testSource),
            auctionObject = None
          )

          val testProtoRequest = PatchPricesRequest(
            ProtoProject.AUTORU,
            PatchPricesRequest.From.FromNow(value = true),
            List(
              PatchPricesRequestEntry(
                product,
                Some(RuleContext(List(RuleCriteria(criterion1key, RuleCriteria.Value.DefinedValue(criterion1value))))),
                Some(Money(prevPrice)),
                PatchPricesRequestEntry.Patch.NextPrice(Money(testPrice))
              )
            ),
            Some(Source(UserRequest(true)))
          )
          assertM(changeRequestToProto.convert(testRequest))(equalTo(testProtoRequest))
        }
      ),
      suite("sourceOptionToProto")(
        testM("should convert None to UserRequest") {
          val testValue = None
          val expected = Source(UserRequest(true))

          assertM(sourceOptionToProto.convert(testValue))(equalTo(expected))

        },
        testM("should convert UserAction to UserRequest") {
          val testValue = Some(AuctionChangeSource.UserAction(testUser))
          val expected = Source(UserRequest(true))

          assertM(sourceOptionToProto.convert(testValue))(equalTo(expected))
        },
        testM("should convert AutoStrategy to ServiceRequest") {
          val autoStrategyId = "123"
          val testValue = Some(AuctionChangeSource.AutoStrategy(autoStrategyId))
          val expected = Source(ServiceRequest(s"auction_auto_strategy:$autoStrategyId"))

          assertM(sourceOptionToProto.convert(testValue))(equalTo(expected))
        },
        testM("should convert TaskRemoveOldBids to ServiceRequest") {
          val request = Some(AuctionChangeSource.TaskRemoveOldBids())
          val expected = Source(ServiceRequest("TaskRemoveOldBids"))
          assertM(sourceOptionToProto.convert(request))(equalTo(expected))
        }
      )
    )
  }

  private val product = "call"
  private val criterion1key = "key"
  private val criterion1value = "value"
  private val entryId = "test1"
  private val prevPrice = 2000
  private val testPrice = 1000
  private val testRule = "rule1"
  private val testUser = UserId("123")
  private val testSource = AuctionChangeSource.UserAction(testUser)
}
