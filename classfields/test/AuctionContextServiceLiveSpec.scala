package vsmoney.auction_auto_strategy.services.test

import vsmoney.auction_auto_strategy.model.auction.{
  AuctionContext,
  AuctionContextId,
  AuctionKey,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project}
import vsmoney.auction_auto_strategy.services.AuctionContextService
import vsmoney.auction_auto_strategy.services.impl.AuctionContextServiceLive
import vsmoney.auction_auto_strategy.services.testkit.AuctionContextDAOMock
import zio.ZIO
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AuctionContextServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionContextServiceLive")(
      suite("getForIds")(
        testM("should return empty list and not call dao if arguments is empty list") {
          val emptyContextDao = AuctionContextDAOMock.empty
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.getForIds(Nil))
          assertM(testRequest.run)(succeeds(equalTo(Nil)))
            .provideCustomLayer(emptyContextDao >>> AuctionContextServiceLive.live)
        },
        testM("should forward result from contextDAO if arguments is not empty list") {
          val contextDao = AuctionContextDAOMock.GetForIds(equalTo(List(testContext.id)), value(List(testContext)))
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.getForIds(List(testContext.id)))
          assertM(testRequest.run)(succeeds(equalTo(List(testContext))))
            .provideCustomLayer(contextDao >>> AuctionContextServiceLive.live)
        }
      ),
      suite("get")(
        testM("should forward request to ContextDAO and forward response from ContextDAO") {
          val contextDao = AuctionContextDAOMock.Get(equalTo(testAuctionKey), value(Some(testContext)))
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.get(testAuctionKey))
          assertM(testRequest.run)(succeeds(equalTo(Some(testContext))))
            .provideCustomLayer(contextDao >>> AuctionContextServiceLive.live)
        }
      ),
      suite("getAll")(
        testM("should forward error from DAO") {
          val contextDao = AuctionContextDAOMock.GetAll(failure(new RuntimeException("test")))
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.getAll)
          assertM(testRequest.run)(fails(isSubtype[RuntimeException](anything)))
            .provideCustomLayer(contextDao >>> AuctionContextServiceLive.live)
        },
        testM("should forward result from DAO") {
          val contextDao = AuctionContextDAOMock.GetAll(value(List(testContext)))
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.getAll)
          assertM(testRequest.run)(succeeds(equalTo(List(testContext))))
            .provideCustomLayer(contextDao >>> AuctionContextServiceLive.live)
        }
      ),
      suite("getOrCreate")(
        testM("should forward request to ContextDAO and forward response from ContextDAO") {
          val contextDao = AuctionContextDAOMock.GetOrCreate(equalTo(testAuctionKey), value(testContext))
          val testRequest = ZIO
            .service[AuctionContextService]
            .flatMap(_.getOrCreate(testAuctionKey))
          assertM(testRequest.run)(succeeds(equalTo(testContext)))
            .provideCustomLayer(contextDao >>> AuctionContextServiceLive.live)
        }
      )
    )
  }

  private val testCriteriaContext = CriteriaContext(criteria =
    Seq(
      Criterion(key = CriterionKey("region_id"), value = CriterionValue("42")),
      Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
      Criterion(key = CriterionKey("model"), value = CriterionValue("x5"))
    )
  )

  private val testAuctionKey = AuctionKey(
    project = Project.Autoru,
    product = ProductId("call"),
    context = testCriteriaContext
  )

  private val testContext = AuctionContext(
    id = AuctionContextId(1999),
    auctionKey = testAuctionKey
  )
}
