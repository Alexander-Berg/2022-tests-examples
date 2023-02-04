package vsmoney.auction.services.test

import billing.common_model.{Money, Project}
import billing.howmuch.model.{Matrix, Rule, RuleContext, RuleCriteria}
import common.models.finance.Money.Kopecks
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import vsmoney.auction.model._
import vsmoney.auction.services.configuration.UsersBidsServiceConfig
import vsmoney.auction.services.impl.UsersBidsServiceLive
import vsmoney.auction.services.testkit.S3EdrReaderMock
import vsmoney.auction.services.{CallCarsNewAuctionDataType, CallCarsUsedAuctionDataType, UsersBidsService}
import zio.{ZIO, ZLayer}
import zio.blocking._
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test.mock.Expectation._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object UsersBidsServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UsersBidsService")(
      testM("should return index based on martix") {
        val mock = S3EdrReaderMock.Data(
          equalTo(CallCarsUsedAuctionDataType),
          value(ZStream.fromIterable(matrixLBU.toByteArray))
        ) ++ S3EdrReaderMock.Data(
          equalTo(CallCarsNewAuctionDataType),
          value(ZStream.fromIterable(matrix.toByteArray))
        )

        val configUserBidsService =
          ZLayer.succeed(UsersBidsServiceConfig(List(CallCarsUsedAuctionDataType, CallCarsNewAuctionDataType)))

        val test =
          for {
            userBids <- ZIO.service[UsersBidsService]
            rs1 <- userBids.get(key1)
            rs2 <- userBids.get(key2)
            rs3 <- userBids.get(key3)
            rs4 <- userBids.get(keyWithOffer)
          } yield {
            assert(rs1)(
              hasSameElements(Seq(UserBid(UserId("1"), Bid(Kopecks(1000))), UserBid(UserId("2"), Bid(Kopecks(2000)))))
            ) &&
            assert(rs2)(hasSameElements(Seq(UserBid(UserId("1"), Bid(Kopecks(1500)))))) &&
            assert(rs3)(hasSameElements(Seq(UserBid(UserId("3"), Bid(Kopecks(500)))))) &&
            assert(rs4)(hasSameElements(Seq(UserBid(UserId("42"), Bid(Kopecks(700))))))
          }

        test.provideLayer(
          (Prometheus.live ++ Blocking.live ++ Random.live ++ Clock.live ++ Logging.live ++ configUserBidsService ++ mock) >>> UsersBidsServiceLive.live
        )
      }
    )

  private val key1 = AuctionKey(
    vsmoney.auction.model.Project.Autoru,
    ProductId("call"),
    CriteriaContext(Seq(Criterion(CriterionKey("mark"), CriterionValue("BMW")))),
    None
  )

  private val key2 = AuctionKey(
    vsmoney.auction.model.Project.Autoru,
    ProductId("call"),
    CriteriaContext(
      Seq(
        Criterion(CriterionKey("mark"), CriterionValue("BMW")),
        Criterion(CriterionKey("model"), CriterionValue("m3"))
      )
    ),
    None
  )

  private val key3 = AuctionKey(
    vsmoney.auction.model.Project.Autoru,
    ProductId("call"),
    CriteriaContext(Seq(Criterion(CriterionKey("mark"), CriterionValue("OPEL")))),
    None
  )

  private val keyWithOffer = AuctionKey(
    vsmoney.auction.model.Project.Autoru,
    ProductId("call:cars:used"),
    CriteriaContext(
      Seq(
        Criterion(CriterionKey("mark"), CriterionValue("BMW")),
        Criterion(CriterionKey("model"), CriterionValue("X5"))
      )
    ),
    Some(Criterion(CriterionKey("offer_id"), CriterionValue("23423-xfss")))
  )

  private val rule1 = createRule(
    List(
      "user_id" -> "1",
      "mark" -> "BMW"
    ),
    1000
  )

  private val rule2 = createRule(
    List(
      "user_id" -> "1",
      "mark" -> "BMW",
      "model" -> "m3"
    ),
    1500
  )

  private val rule3 = createRule(
    List(
      "user_id" -> "2",
      "mark" -> "BMW"
    ),
    2000
  )

  private val rule4 = createRule(
    List(
      "user_id" -> "3",
      "mark" -> "OPEL"
    ),
    500
  )

  private val offerRule = createRule(
    List(
      "user_id" -> "42",
      "mark" -> "BMW",
      "model" -> "X5",
      "offer_id" -> "23423-xfss"
    ),
    700
  )

  private val matrix = Matrix(Project.AUTORU, "call_auction", Seq(rule1, rule2, rule3, rule4))
  private val matrixLBU = Matrix(Project.AUTORU, "call:cars:used", Seq(offerRule))

  private def createRule(context: List[(String, String)], price: Long) =
    Rule(
      Some(RuleContext(context.map { case (key, value) => RuleCriteria(key, RuleCriteria.Value.DefinedValue(value)) })),
      Some(Money(price))
    )

}
