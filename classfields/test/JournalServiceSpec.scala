package vsmoney.auction.services.test

import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf
import common.tagged.tag
import common.tagged.tag.@@
import common.zio.events_broker.Broker.{OtherError, TooManyRequests}
import ru.yandex.vertis.broker.api.common.EventWriteErrorCodeMessage.EventWriteErrorCode.UNEXPECTED_ERROR
import vsmoney.auction.auction_bids_delivery.AuctionBid
import vsmoney.auction.common_model.{ChangeSource, Project => ProtoProject}
import vsmoney.auction.model._
import vsmoney.auction.model.request.{LeaveAuctionEntityRequest, PlaceBidBEntityRequest}
import vsmoney.auction.services.{BidEventJournal, JournalService}
import vsmoney.auction.services.BidEventJournal.{AuctionStopEvent, BidEvent}
import vsmoney.auction.services.impl.{BrokerBidEventJournal, JournalServiceLive}
import vsmoney.auction.services.impl.BrokerBidEventJournal.Broker
import vsmoney.auction.services.impl.KafkaBidEventJournal.Kafka
import vsmoney.auction.services.testkit.{BidBrokerMock, BidEventJournalMock}
import zio.{Has, ZIO}
import zio.duration._
import zio.test.Assertion._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.mock.Expectation.{failure, unit}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.OffsetDateTime

object JournalServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("JournalService")(
      testM("placeBidBatch") {
        val result = for {
          service <- ZIO.service[JournalService]
          _ <- service.placeBidBatch(List(PlaceBidBEntityRequest(testAuction, Bid(testBid), None)), Some(testSource))
        } yield ()

        val brokerMock = BidEventJournalMock
          .BidPlaced(equalTo(Set(BidEvent(testAuction, Bid(testBid), Some(testSource)))), unit)
          .toLayer
          .map(a => Has(tag[Broker][BidEventJournal](a.get[BidEventJournal])))
        val kafkaMock = BidEventJournalMock
          .BidPlaced(equalTo(Set(BidEvent(testAuction, Bid(testBid), Some(testSource)))), unit)
          .toLayer
          .map(a => Has(tag[Kafka][BidEventJournal](a.get[BidEventJournal])))
        assertM(result)(isUnit).provideSomeLayer(
          brokerMock ++ kafkaMock >>> JournalServiceLive.live
        )
      },
      testM("stopBatchAuction") {
        val result = for {
          service <- ZIO.service[JournalService]
          _ <- service.stopBatchAuction(List(LeaveAuctionEntityRequest(testAuction, Bid(testBid))), Some(testSource))
        } yield ()

        val brokerMock = BidEventJournalMock
          .AuctionStopped(equalTo(Set(AuctionStopEvent(testAuction, Some(testSource)))), unit)
          .toLayer
          .map(a => Has(tag[Broker][BidEventJournal](a.get[BidEventJournal])))
        val kafkaMock = BidEventJournalMock
          .AuctionStopped(equalTo(Set(AuctionStopEvent(testAuction, Some(testSource)))), unit)
          .toLayer
          .map(a => Has(tag[Kafka][BidEventJournal](a.get[BidEventJournal])))
        assertM(result)(isUnit).provideSomeLayer(
          brokerMock ++ kafkaMock >>> JournalServiceLive.live
        )
      }
    )
  }

  private val product = ProductId("call")
  private val project = Project.Autoru
  private val user = UserId("user:1")
  private val testBid = Kopecks(2000)

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

}
