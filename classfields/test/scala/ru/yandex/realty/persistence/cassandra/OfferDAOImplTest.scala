package ru.yandex.realty.persistence.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.application.ng.{DefaultMdsUrlBuilderSupplier, MdsUrlBuilderConfig}
import ru.yandex.realty.model.serialization.MockOfferBuilder

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 27.05.16
  */
class OfferDAOImplTest
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with OfferSupport
  with DefaultMdsUrlBuilderSupplier
  with IndexerCassandraSession {

  override def mdsUrlBuilderConfig: MdsUrlBuilderConfig = ???

  override def indexerCassandraSessionConfig: CassandraSessionConfig = ???

  val db = new CassandraAPI(indexerCassandraSession, mdsUrlBuilder)

  implicit val config = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(300, Millis)))

  "OfferDAO" should "should return empty result for unknown offer" in {
    db.getRawOffer(Some(0), "-1").futureValue should equal(None)
  }

  it should "correct upsert and remove offers" in {
    val offerId = "777"
    val groupId = 1
    val raw = buildRawOffer(offerId)

    db.upsertRawOffer(groupId, raw).futureValue

    db.getRawOffer(None, offerId).futureValue.get.getId should be(offerId)
    db.getRawOffer(Some(groupId), offerId).futureValue.get.getId should be(offerId)

    val offer = MockOfferBuilder.createMockOffer()
    offer.setId(offerId.toLong)

    db.upsertUnifiedOffer(groupId, offer).futureValue
    db.getOffer(None, offerId).futureValue.get.getId should be(offerId)
    db.getOffer(Some(groupId), offerId).futureValue.get.getId should be(offerId)

    // check cluster search
    val allOffersInGroup = db.getGroupOffers(groupId).futureValue
    allOffersInGroup should not be empty
    allOffersInGroup.find(_.getId == offerId) should not be empty

    // check remove
    db.deleteOffer(None, offerId).futureValue

    db.getRawOffer(None, offerId).futureValue should be(None)
    db.getRawOffer(Some(groupId), offerId).futureValue should be(None)
    db.getOffer(None, offerId).futureValue should be(None)
    db.getOffer(Some(groupId), offerId).futureValue should be(None)
  }

  it should "correct move to new group" in {
    val offerId = "777"
    val groupId = 1
    val raw = buildRawOffer(offerId)

    db.upsertRawOffer(groupId, raw).futureValue
    db.getRawOffer(None, offerId).futureValue.get.getId should be(offerId)

    val offer = MockOfferBuilder.createMockOffer()
    offer.setId(offerId.toLong)
    db.upsertUnifiedOffer(groupId, offer).futureValue
    db.getOffer(Some(groupId), offerId).futureValue.get.getId should be(offerId)

    val groupId2 = 2
    db.upsertRawOffer(groupId2, raw).futureValue

    db.getRawOffer(Some(groupId), offerId).futureValue should be(None)
    db.getOffer(Some(groupId), offerId).futureValue should be(None)
    db.getRawOffer(None, offerId).futureValue.get.getId should be(offerId)
    db.getRawOffer(Some(groupId2), offerId).futureValue.get.getId should be(offerId)
    db.getOffer(None, offerId).futureValue.get.getId should be(offerId)
    db.getOffer(Some(groupId2), offerId).futureValue.get.getId should be(offerId)

    val groupId3 = 3
    db.moveOfferBytes(db.getIndexingBytes(offerId).futureValue.get, groupId3, offerId).futureValue
    db.getRawOffer(Some(groupId2), offerId).futureValue should be(None)
    db.getOffer(Some(groupId2), offerId).futureValue should be(None)
    db.getRawOffer(None, offerId).futureValue.get.getId should be(offerId)
    db.getRawOffer(Some(groupId3), offerId).futureValue.get.getId should be(offerId)
    db.getOffer(Some(groupId3), offerId).futureValue should be(None)
  }

  it should "correct work with full scan for raw offer" in {
    val offerId = "777"
    val groupId = 1
    val raw = buildRawOffer(offerId)

    db.upsertRawOffer(groupId, raw).futureValue

    val allOffers = db.getAllRawOffers.futureValue
    val first10 = allOffers.take(10)
    first10 should not be empty

    db.deleteOffer(None, offerId).futureValue
  }
}
