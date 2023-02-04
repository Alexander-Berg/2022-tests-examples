package ru.yandex.realty.persistence.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.RealtimeCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.persistence.model.UnifiedOffer
import ru.yandex.realty.pos.TestOperationalComponents

import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by abulychev on 19.07.16.
  */
class RealtimeOfferDAOImplTest
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with RealtimeCassandraSession
  with TestOperationalComponents {

  override def realtimeCassandraSessionConfig: CassandraSessionConfig = ???

  val db = new RealtimeCassandraAPI(realtimeCassandraSession)(ops)

  implicit val config = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(300, Millis)))

  def buildOffer(offerId: String): Offer = {
    val o = new Offer()
    o.setId(offerId.toLong)
    o
  }

  "RealtimeOfferDAO" should "return none to unknown offer" in {
    db.readSync("99999999") should equal(Success(None))
  }

  it should "should write batch of offers" in {
    val offers = Seq(buildOffer("555"), buildOffer("111"), buildOffer("666"))
    db.upsert(offers.map(UnifiedOffer.apply)).futureValue
    db.readSync("555").get.get._1.getId should equal("555")
    db.readSync("111").get.get._1.getId should equal("111")
    db.readSync("666").get.get._1.getId should equal("666")
  }

}
