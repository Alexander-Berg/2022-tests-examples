package vsmoney.auction.services.test

import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf
import common.tagged.tag.@@
import common.zio.ops.tracing.testkit.TestTracing
import org.apache.kafka.clients.producer.ProducerRecord
import vsmoney.auction.auction_bids_delivery.AuctionBid
import vsmoney.auction.common_model.{ChangeSource, Project => ProtoProject}
import vsmoney.auction.model._
import vsmoney.auction.services.BidEventJournal
import vsmoney.auction.services.BidEventJournal.{AuctionStopEvent, BidEvent}
import vsmoney.auction.services.impl.KafkaBidEventJournal
import vsmoney.auction.services.impl.KafkaBidEventJournal.{Kafka, KafkaAuctionBidsTopic}
import vsmoney.auction.services.testkit.BidKafkaProducerMock
import zio.blocking.Blocking
import zio.duration._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test._
import zio.{ZIO, ZLayer}

import java.time.OffsetDateTime

object KafkaBidEventJournalSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("KafkaBidEventJournal")(
      testM("should send bid to kafka") {

        (for {
          _ <- TestClock.setDateTime(testTime)
          service <- ZIO.service[BidEventJournal @@ Kafka]
          _ <- service.bidPlaced(Set(BidEvent(testAuction, Bid(testBid), Some(testSource))))
          produced <- BidKafkaProducerMock.records[String, AuctionBid]
          expected = new ProducerRecord[String, AuctionBid]("topic", user.id, testEvent)
        } yield assertTrue(produced.size == 1 && produced.head == expected))
          .provideSomeLayer(
            TestTracing.mock ++ Blocking.any ++ TestClock.any ++
              ZLayer.succeed(KafkaAuctionBidsTopic("topic")) ++
              BidKafkaProducerMock.mock() >+> KafkaBidEventJournal.live
          )
      },
      testM("should send bid to kafka at second try") {
        (for {
          _ <- TestClock.setDateTime(testTime)
          service <- ZIO.service[BidEventJournal @@ Kafka]
          fiber <- service.bidPlaced(Set(BidEvent(testAuction, Bid(testBid), Some(testSource)))).fork
          _ <- TestClock.adjust(1.minute)
          _ <- fiber.join
          produced <- BidKafkaProducerMock.records[String, AuctionBid]
          expected = new ProducerRecord[String, AuctionBid]("topic", user.id, testEvent)
        } yield assertTrue(produced.size >= 2 && produced.head == expected))
          .provideSomeLayer(
            TestTracing.mock ++ Blocking.any ++ TestClock.any ++ ZLayer.succeed(
              KafkaAuctionBidsTopic("topic")
            ) ++ BidKafkaProducerMock.mock(true) >+> KafkaBidEventJournal.live
          )
      },
      testM("should send stop bid to kafka") {
        (for {
          _ <- TestClock.setDateTime(testTime)
          service <- ZIO.service[BidEventJournal @@ Kafka]
          _ <- service.auctionStopped(Set(AuctionStopEvent(testAuction, Some(testSource))))
          produced <- BidKafkaProducerMock.records[String, AuctionBid]
          expected = new ProducerRecord[String, AuctionBid]("topic", user.id, testEvent.withBid(0))
        } yield assertTrue(produced.size == 1 && produced.head == expected))
          .provideSomeLayer(
            TestTracing.mock ++ Blocking.any ++ TestClock.any ++ ZLayer.succeed(
              KafkaAuctionBidsTopic("topic")
            ) ++ BidKafkaProducerMock.mock() >+> KafkaBidEventJournal.live
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
