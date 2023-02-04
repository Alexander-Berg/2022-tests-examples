package vsmoney.auction.services.test

import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf
import common.tagged.tag.@@
import common.zio.events_broker.Broker.{OtherError, TooManyRequests}
import ru.yandex.vertis.broker.api.common.EventWriteErrorCodeMessage.EventWriteErrorCode.UNEXPECTED_ERROR
import vsmoney.auction.auction_bids_delivery.AuctionBid
import vsmoney.auction.model.{
  AuctionChangeSource,
  AuctionKey,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  ProductId,
  Project,
  UserAuction,
  UserId
}
import vsmoney.auction.services.BidEventJournal
import vsmoney.auction.services.impl.BrokerBidEventJournal
import vsmoney.auction.services.testkit.BidBrokerMock
import vsmoney.auction.common_model.{ChangeSource, Project => ProtoProject}
import vsmoney.auction.services.BidEventJournal.{AuctionStopEvent, BidEvent}
import vsmoney.auction.services.impl.BrokerBidEventJournal.Broker
import zio.ZIO
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test.mock.Expectation.{failure, unit}
import zio.duration._

import java.time.OffsetDateTime

object BrokerBidEventJournalSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("BrokerBidEventJournal")(
      testM("should send bid to broker") {
        val result = for {
          _ <- TestClock.setDateTime(testTime)
          service <- ZIO.service[BidEventJournal @@ Broker]
          _ <- service.bidPlaced(Set(BidEvent(testAuction, Bid(testBid), Some(testSource))))
        } yield ()

        val brokerMock = BidBrokerMock.Send(equalTo((testEvent, None, None)), unit)

        assertM(result)(isUnit).provideSomeLayer(
          TestClock.any ++ brokerMock >>> BrokerBidEventJournal.live
        )
      },
      testM("should send stop auction to broker") {
        val result = for {
          _ <- TestClock.setDateTime(testTime)
          service <- ZIO.service[BidEventJournal @@ Broker]
          _ <- service.auctionStopped(Set(AuctionStopEvent(testAuction, Some(testSource))))
        } yield ()

        val expectedEvent = testEvent.copy(bid = 0)
        val brokerMock = BidBrokerMock.Send(equalTo((expectedEvent, None, None)), unit)

        assertM(result)(isUnit).provideSomeLayer(
          TestClock.any ++ brokerMock >>> BrokerBidEventJournal.live
        )
      },
      testM("retry once on OtherError from broker than return unit") {
        val result = for {
          service <- ZIO.service[BidEventJournal @@ Broker]
          fiber <- service.bidPlaced(Set(BidEvent(testAuction, Bid(testBid), Some(testSource)))).fork
          _ <- TestClock.adjust(1.minute)
          _ <- fiber.join
        } yield ()

        val brokerMock = BidBrokerMock.Send(anything, failure(OtherError(UNEXPECTED_ERROR, "bla"))).atLeast(2)

        assertM(result)(isUnit).provideSomeLayer(
          TestClock.any ++ brokerMock >>> BrokerBidEventJournal.live
        )
      },
      testM("not fails on errors differ from OtherError") {
        val result = for {
          service <- ZIO.service[BidEventJournal @@ Broker]
          _ <- service.bidPlaced(Set(BidEvent(testAuction, Bid(testBid), Some(testSource))))
        } yield ()

        val brokerMock = BidBrokerMock.Send(anything, failure(TooManyRequests("bla")))

        assertM(result)(isUnit).provideSomeLayer(
          TestClock.any ++ brokerMock >>> BrokerBidEventJournal.live
        )
      }
    )
  }

  private val product = ProductId("call")
  private val project = Project.Autoru
  private val user = UserId("user:1")
  private val testBid = Kopecks(2000)
  private val testTime = OffsetDateTime.parse("2021-07-09T19:34+03:00")

  private val testContext = CriteriaContext(
    List(Criterion(CriterionKey("key"), CriterionValue("value")))
  )

  private val testAuction = UserAuction(
    AuctionKey(
      project,
      product,
      testContext,
      auctionObject = None
    ),
    user
  )

  private val testSource = AuctionChangeSource.UserAction(user)

  private val testEvent = AuctionBid(
    Some(ScalaProtobuf.instantToTimestamp(testTime.toInstant)),
    ProtoProject.AUTORU,
    product.id,
    user.id,
    "user_id=user:1&key=value",
    testBid.value,
    Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user.id))))
  )
}
