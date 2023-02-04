package ru.auto.cabinet.tasks.impl.tagging

import java.time.OffsetDateTime
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatcher
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpecLike
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.cabinet.model.ClientId
import ru.auto.cabinet.model.offer.{OfferId, OfferTag}
import ru.auto.cabinet.service.dealer_stats.DealerStatsClient.Metric
import ru.auto.cabinet.trace.Context

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.Random

trait TaggingTaskSpec
    extends AnyFlatSpecLike
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with OneInstancePerTest
    with VasTags
    with BeforeAndAfterAll {
  implicit protected val rc = Context.unknown

  implicit val as: ActorSystem =
    ActorSystem.create("test", ConfigFactory.empty())

  override protected def afterAll(): Unit = {
    super.afterAll()
    as.terminate()
  }

  protected def mockOffer(offerId: OfferId, tags: Seq[OfferTag] = Nil): Offer =
    Offer.newBuilder().setId(offerId).addAllTags(tags.asJava).build()

  protected def futureOptOffer(
      offerId: OfferId,
      tags: OfferTag*): Future[Some[Offer]] =
    Future.successful(Some(mockOffer(offerId, tags)))

  protected def futureOffer(
      id: OfferId,
      creationDate: Long,
      tags: OfferTag*): Future[Offer] = {
    Future.successful {
      val builder = Offer.newBuilder()
      builder.setId(id)
      builder.getAdditionalInfoBuilder.setCreationDate(creationDate)
      builder.addAllTags(tags.asJava).build()
    }
  }

  protected val futureUnit: Future[Unit] = Future.successful(())

  protected def futureClientIds(clientIds: ClientId*): Future[Seq[ClientId]] =
    Future.successful(clientIds)

  protected def futureMetrics(metrics: Metric*): Future[List[Metric]] =
    Future.successful(Random.shuffle(metrics).toList)

  protected val ClientId1 = 58L
  protected val ClientId2 = 59L
  protected val ClientId3 = 60L
  protected val ClientId4 = 61L

  protected val LastTagging = Some(
    OffsetDateTime.parse("2017-01-01T00:00+03:00"))
  protected val Offer1Id = "1043652664-4a1ab"
  protected val Offer1IdWithUndefinedHash = "1043652664-undefined"
  protected val Offer1IdWithoutHash = "1043652644"

  protected val Offer1Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-01-02T00:00+03:00")
  protected val Offer1Millis: ClientId = Offer1Odt.toInstant.toEpochMilli
  protected val Offer2Id = "1043652665-4a1ac"

  protected val Offer2Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-01-03T00:00+03:00")
  protected val Offer2Millis: ClientId = Offer2Odt.toInstant.toEpochMilli
  protected val Offer3Id = "1043652666-4a1ad"

  protected val Offer3Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-02-03T00:00+03:00")
  protected val Offer3Millis: ClientId = Offer3Odt.toInstant.toEpochMilli
  protected val Offer4Id = "1043652567-4a1ae"

  protected val Offer4Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-02-02T00:00+03:00")
  protected val Offer4Millis: ClientId = Offer4Odt.toInstant.toEpochMilli
  protected val Offer5Id = "1043652468-4a1af"

  protected val Offer5Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-05-03T00:00+03:00")
  protected val Offer5Millis: ClientId = Offer5Odt.toInstant.toEpochMilli
  protected val Offer6Id = "1043652369-4a1ba"

  protected val Offer6Odt: OffsetDateTime =
    OffsetDateTime.parse("2017-03-03T00:00+03:00")
  protected val Offer6Millis: ClientId = Offer6Odt.toInstant.toEpochMilli

  protected val testStart: OffsetDateTime = OffsetDateTime.now()
  protected val strTestStart: OfferId = testStart.toString

  protected val strAfterTestStart = new ArgumentMatcher[String] {
    override def matches(argument: String): Boolean = argument >= strTestStart
  }

  protected val afterTestStart = new ArgumentMatcher[OffsetDateTime] {

    override def matches(argument: OffsetDateTime): Boolean =
      argument.isAfter(testStart)
  }

  implicit override val patienceConfig = PatienceConfig(Span(5, Seconds))
}
