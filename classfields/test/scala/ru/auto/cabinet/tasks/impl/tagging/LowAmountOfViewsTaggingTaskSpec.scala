package ru.auto.cabinet.tasks.impl.tagging

import java.time.{LocalDate, ZoneId}
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Offer, Seller}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.PaidService
import ru.auto.cabinet.dao.jdbc.{JdbcClientDao, JdbcKeyValueDao}
import ru.auto.cabinet.service.dealer_stats.DealerStatsClient
import ru.auto.cabinet.service.statist.{
  Component,
  Counter,
  Domain,
  StatistClient
}
import ru.auto.cabinet.service.statist.StatistClient.NumericOfferId
import ru.auto.cabinet.service.vos.VosClient
import ru.auto.cabinet.tasks.impl.tagging.LowAmountOfViewsTaggingTask._
import ru.auto.dealer_stats.proto.Model.{Day, DealerStatsResponse}
import ru.yandex.vertis.statist.model.api.ApiModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/** Testing of [[LowAmountOfViewsTaggingTask]]
  */
class LowAmountOfViewsTaggingTaskSpec
    extends TaggingTaskSpec
    with VasTags
    with ScalaFutures {

  behavior.of("LowAmountOfViewsTaggingTask")

  private val AvgViews = 100
  private val statistClient = mock[StatistClient]
  private val dealerStatsClient = mock[DealerStatsClient]
  private val clientDao = mock[JdbcClientDao]
  private val keyValue = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]

  val now = LocalDate.now(ZoneId.of("UTC"))

  val task = new LowAmountOfViewsTaggingTask(
    statistClient,
    dealerStatsClient,
    clientDao,
    keyValue,
    vosClient) with TestInstrumented

  val avgMetrics = DealerStatsResponse
    .newBuilder()
    .addAllDays {
      Seq(
        Day
          .newBuilder()
          .setDay(now.minusDays(1).toString)
          .setCardViews(AvgViews)
          .setPhoneViews(AvgViews)
          .build(),
        Day
          .newBuilder()
          .setDay(now.minusDays(2).toString)
          .setCardViews(AvgViews)
          .setPhoneViews(AvgViews)
          .build(),
        Day
          .newBuilder()
          .setDay(now.minusDays(3).toString)
          .setCardViews(AvgViews)
          .setPhoneViews(AvgViews)
          .build()
      ).asJava
    }
    .build()

  it should "tag when service activated more than delay + 1 days ago" in {

    val mark = "LADA"
    val model = "CALINA"
    val regionId = 3
    // ids requested first
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))

    // then offers is requested via vos
    when(
      vosClient
        .getOffers(argEq(ClientId1), any(), any(), any(), any(), any(), any())(
          any()))
      .thenReturn(Future(Iterable(
        autoOffer(mark, model, regionId, ServiceRecommendationDaysDelay + 1))))

    val actualMetrics =
      ApiModel.MultipleDailyValues
        .newBuilder()
        .putObjects(Offer1Id, objectDailyValues.build())
        .build()

    when(
      statistClient.getCountersMultiple(
        Domain.AutoruPublic,
        Counter.EventTypePerCardByDay,
        NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity),
        Component.CardView,
        now.minusDays(ServiceRecommendationDaysDelay),
        now
      )
    ).thenReturn(Future.successful(actualMetrics))

    when(dealerStatsClient.getAverages(any())(any(), any(), any(), any()))
      .thenReturn(Future(avgMetrics))

    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue

    verify(vosClient).putTag(Offer1Id, LowAmountOfViewsAcrossCompetitors)
  }

  it should " not tag when views ratio is more than bound" in {

    val mark = "LADA"
    val model = "CALINA"
    val regionId = 3
    // ids requested first
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))

    // then offers is requested via vos
    when(
      vosClient
        .getOffers(argEq(ClientId1), any(), any(), any(), any(), any(), any())(
          any()))
      .thenReturn(Future(Iterable(
        autoOffer(mark, model, regionId, ServiceRecommendationDaysDelay + 1))))

    val extraRatio =
      ApiModel.ObjectDayValues
        .newBuilder()
        .setDay(now.toString)
        .putComponents("card_view", (TagRatioBound * AvgViews).toInt)

    val actualMetrics =
      ApiModel.MultipleDailyValues
        .newBuilder()
        .putObjects(Offer1Id, objectDailyValues.setDays(0, extraRatio).build())
        .build()

    when(
      statistClient.getCountersMultiple(
        Domain.AutoruPublic,
        Counter.EventTypePerCardByDay,
        NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity),
        Component.CardView,
        now.minusDays(ServiceRecommendationDaysDelay),
        now
      )
    ).thenReturn(Future.successful(actualMetrics))

    when(dealerStatsClient.getAverages(any())(any(), any(), any(), any()))
      .thenReturn(Future(avgMetrics))

    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue

    verify(vosClient, times(0))
      .putTag(Offer1Id, LowAmountOfViewsAcrossCompetitors)
  }

  it should " not tag when can't find a metric for a day" in {

    val mark = "LADA"
    val model = "CALINA"
    val regionId = 3
    // ids requested first
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))

    // then offers is requested via vos
    when(
      vosClient
        .getOffers(argEq(ClientId1), any(), any(), any(), any(), any(), any())(
          any()))
      .thenReturn(Future(Iterable(
        autoOffer(mark, model, regionId, ServiceRecommendationDaysDelay + 1))))

    val actualMetrics =
      ApiModel.MultipleDailyValues
        .newBuilder()
        .putObjects(Offer1Id, objectDailyValues.removeDays(0).build())
        .build()

    when(
      statistClient.getCountersMultiple(
        Domain.AutoruPublic,
        Counter.EventTypePerCardByDay,
        NumericOfferId.fromOfferId(Offer1Id).fold(throw _, identity),
        Component.CardView,
        now.minusDays(ServiceRecommendationDaysDelay),
        now
      )
    ).thenReturn(Future.successful(actualMetrics))

    when(dealerStatsClient.getAverages(any())(any(), any(), any(), any()))
      .thenReturn(Future(avgMetrics))

    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    task.doTagging(None).futureValue

    verify(vosClient, times(0))
      .putTag(Offer1Id, LowAmountOfViewsAcrossCompetitors)
  }

  private def autoOffer(
      mark: String,
      model: String,
      regionId: Long,
      daysAgoActivation: Int) = {
    Offer
      .newBuilder()
      .setId(Offer1Id)
      .setUserRef(s"dealer:$ClientId1")
      .setCarInfo(CarInfo.newBuilder().setMark(mark).setModel(model))
      .setSeller(
        Seller
          .newBuilder()
          .setLocation(
            ApiOfferModel.Location.newBuilder().setGeobaseId(regionId))
      )
      .addServices(
        PaidService
          .newBuilder()
          .setCreateDate(
            System.currentTimeMillis() - daysAgoActivation.days.toMillis
          )
      )
      .build()
  }

  def objectDailyValues: ApiModel.ObjectDailyValues.Builder = {
    val builder = ApiModel.ObjectDailyValues.newBuilder()

    (1 to EvaluationDelayDays).map { i =>
      builder.addDays {
        ApiModel.ObjectDayValues
          .newBuilder()
          .setDay(now.minusDays(i).toString)
          .putComponents("card_view", (TagRatioBound * AvgViews).toInt)
      }
    }

    builder
  }
}
