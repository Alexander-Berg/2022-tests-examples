package ru.auto.cabinet.tasks.impl.tagging

import org.mockito.ArgumentMatchers.{eq => argEq, _}
import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.cabinet.dao.jdbc.{JdbcClientDao, JdbcKeyValueDao}
import ru.auto.cabinet.environment._
import ru.auto.cabinet.model.offer.{OfferId, VosOfferCategories}
import ru.auto.cabinet.service.statist.StatistClient.NumericOfferId
import ru.auto.cabinet.service.statist.{Component, StatistClient}
import ru.auto.cabinet.service.vos.VosClient
import ru.auto.cabinet.test.TestUtil._
import ru.yandex.vertis.statist.model.api.ApiModel.{
  MultipleDailyValues,
  ObjectDailyValues,
  ObjectDayValues
}
import cats.data.NonEmptyList
import ru.auto.cabinet.service.dealer_stats.DealerStatsClient.Metric

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class FallViewsTaggingTaskSpec extends TaggingTaskSpec {

  private val clientDao = mock[JdbcClientDao]

  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]
  private val statistClient = mock[StatistClient]
  private val fallViewsMetric = FallViewsMetric.Card

  private val task =
    new FallViewsTaggingTask(
      clientDao,
      statistClient,
      keyValueDao,
      vosClient,
      fallViewsMetric,
      minFallPeriodDays = 2,
      maxFallPeriodDays = 7,
      fallCoeff = 0.95,
      minMetricsInPeriodCoeff = 0.15
    ) with TestInstrumented

  private val tag = "fallviewscard"

  private val startOfDay = startOfToday
  private val weekAgo = startOfDay.minusWeeks(1)
  private val twoWeeksAgo = startOfDay.minusWeeks(2)
  private val inPrevious = twoWeeksAgo.plusDays(1)
  private val inCurrent = weekAgo.plusDays(1)

  private def offer(id: OfferId, createdDaysAgo: Int) = {
    val builder = Offer.newBuilder(mockOffer(id))
    builder.getAdditionalInfoBuilder
      .setCreationDate(
        startOfDay
          .minusDays(createdDaysAgo)
          .toInstant
          .toEpochMilli)
    builder.build()
  }

  val Offer1 = offer(Offer1Id, createdDaysAgo = 28)
  val Offer2 = offer(Offer2Id, createdDaysAgo = 28)
  val Offer3 = offer(Offer3Id, createdDaysAgo = 28)
  val Offer4 = offer(Offer4Id, createdDaysAgo = 28)

  private def inverseStatist(
      xs: List[(Offer, List[Metric])]): MultipleDailyValues =
    MultipleDailyValues
      .newBuilder()
      .putAllObjects(
        xs.map { case (offer, metrics) =>
          StatistClient.NumericOfferId
            .fromOfferId(offer.getId)
            .fold(throw _, _.value.toString) -> ObjectDailyValues
            .newBuilder()
            .addAllDays(
              metrics
                .map(metric =>
                  ObjectDayValues
                    .newBuilder()
                    .putComponents(
                      Component.CardView.toString,
                      metric.value.toInt)
                    .setDay(metric.metricDatetime.toLocalDate.toString)
                    .build())
                .asJava
            )
            .build()
        }.toMap
          .asJava
      )
      .build()

  private def futureOldActiveOffer(id: OfferId) =
    Future.successful(Some(offer(id, createdDaysAgo = 28)))

  private def newActiveOffer(id: OfferId) = offer(id, createdDaysAgo = 5)

  "FailViewsTaggingTaskSpec" should "tag some of client offers" in {
    when(clientDao.getActiveClientIds(any()))
      .thenReturn(futureClientIds(ClientId1, ClientId2, ClientId3, ClientId4))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturnF(List(Offer1, Offer2))

    when(
      vosClient.getOffers(
        argEq(ClientId2),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturnF(List(Offer3))

    when(
      vosClient.getOffers(
        argEq(ClientId3),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturnF(List(Offer4))

    when(
      vosClient.getOffers(
        argEq(ClientId4),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturnF(List.empty)

    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(
          NonEmptyList
            .of(
              NumericOfferId.fromOfferId(Offer1Id),
              NumericOfferId.fromOfferId(Offer2Id)
            )
            .map(_.fold(throw _, identity))),
        any(),
        any(),
        any()
      )(any()))
      .thenReturnF(inverseStatist(List(
        (
          Offer1,
          List(
            Metric(
              s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
              inPrevious,
              15),
            Metric(
              s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
              inPrevious,
              10),
            Metric(
              s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
              inCurrent,
              18),
            Metric(
              s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
              inCurrent,
              1)
          )),
        (
          Offer2,
          List(
            Metric(
              s"dealer.$ClientId1.offer.$Offer2Id.card.view.count",
              inCurrent,
              20),
            Metric(
              s"dealer.$ClientId1.offer.$Offer2Id.card.view.count",
              inCurrent,
              20),
            Metric(
              s"dealer.$ClientId1.offer.$Offer2Id.card.view.count",
              inPrevious,
              20)
          ))
      )))

    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer3Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturnF(
        inverseStatist(
          List(
            (
              Offer3,
              List(
                Metric(
                  s"dealer.$ClientId2.offer.$Offer3Id.card.view.count",
                  inPrevious,
                  5)
              )))))

    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer4Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturnF(
        inverseStatist(
          List(
            (
              Offer4,
              List(
                Metric(
                  s"dealer.$ClientId3.offer.$Offer4Id.card.view.count",
                  inPrevious,
                  40),
                Metric(
                  s"dealer.$ClientId3.offer.$Offer4Id.card.view.count",
                  inCurrent,
                  39)
              ))
          )))
    when(vosClient.putTag(any(), argEq(tag))(any())).thenReturn(futureUnit)
    when(vosClient.deleteTag(any(), argEq(tag))(any())).thenReturn(futureUnit)
    // don't deal with tagging times in this task
    task.doTagging(None).futureValue shouldBe None
    // current period for offer1 contains less card views
    verify(vosClient).putTag(Offer1Id, tag)
    // current period for offer2 contains more card views, so, untag it
    verify(vosClient).deleteTag(Offer2Id, tag)
    // previous period has more views, than current
    verify(vosClient).putTag(Offer3Id, tag)
    // current period for offer4 contains only less than 5% less card views, so, untag it
    verify(vosClient).deleteTag(Offer4Id, tag)
    // badofferid has old format without hyphen, so, don't tag it
    verify(vosClient, atLeastOnce()).getOffers(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any())(any())
  }

  it should "untag offer with current metrics & without previous metrics" in {
    when(clientDao.getActiveClientIds(any()))
      .thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturn(Future.successful(List(Offer1)))
    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturn(
        Future.successful(
          inverseStatist(
            List(
              (
                Offer1,
                List(
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    inCurrent,
                    5))))
          )))
    when(vosClient.getOffer(any())(any()))
      .thenReturn(futureOldActiveOffer(Offer1Id))
    when(vosClient.deleteTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue shouldBe None
    verify(vosClient).deleteTag(Offer1Id, tag)
    verify(vosClient).getOffers(ClientId1, VosOfferCategories.All)
  }

  it should "tag offer with previous metrics & without current metrics" in {
    when(clientDao.getActiveClientIds(any()))
      .thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturn(Future.successful(List(Offer1)))
    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturn(
        Future.successful(
          inverseStatist(
            List(
              (
                Offer1,
                List(
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    inPrevious,
                    5))))
          )))
    when(vosClient.getOffer(any())(any()))
      .thenReturn(futureOldActiveOffer(Offer1Id))
    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue shouldBe None
    verify(vosClient).putTag(Offer1Id, tag)
    verify(vosClient).getOffers(ClientId1, VosOfferCategories.All)
  }

  it should "not do anything if there are both no previous & current metrics" in {
    when(clientDao.getActiveClientIds(rc))
      .thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturn(Future.successful(List(Offer1)))
    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturn(
        Future.successful(
          inverseStatist(
            List((Offer1, List.empty))
          )))
    task.doTagging(None)(rc).futureValue shouldBe None
    verify(vosClient, never()).putTag(any(), any())(any())
    verify(vosClient, never()).putTags(any(), any())(any())
    verify(vosClient, never()).deleteTag(any(), any())(any())
    verify(vosClient, never()).deleteTags(any(), any())(any())
  }

  it should "split metrics into periods of less size" in {
    when(clientDao.getActiveClientIds(any()))
      .thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        argEq(VosOfferCategories.All),
        any(),
        any(),
        any(),
        any(),
        any())(any()))
      .thenReturnF(List(newActiveOffer(Offer1Id)))
    when(
      statistClient.getCountersMultipleBatch(
        any(),
        any(),
        argEq(NonEmptyList.of(
          NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity))),
        any(),
        any(),
        any())(any()))
      .thenReturn(
        Future.successful(
          inverseStatist(
            List(
              (
                Offer1,
                List(
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    startOfDay.minusDays(4),
                    5),
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    startOfDay.minusDays(3),
                    20),
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    startOfDay.minusDays(2),
                    5),
                  Metric(
                    s"dealer.$ClientId1.offer.$Offer1Id.card.view.count",
                    startOfDay.minusDays(1),
                    5)
                ))))))
    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue shouldBe None
    verify(vosClient).getOffers(ClientId1, VosOfferCategories.All)
    verify(vosClient).putTag(Offer1Id, tag)
  }
}
