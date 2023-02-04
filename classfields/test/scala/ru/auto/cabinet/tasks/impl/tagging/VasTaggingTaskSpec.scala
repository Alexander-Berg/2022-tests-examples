package ru.auto.cabinet.tasks.impl.tagging

import org.mockito.ArgumentMatchers.{eq => argEq, _}
import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel
import ru.auto.cabinet.dao.entities.vas.{
  Capital,
  Mark,
  Model,
  RecommendationLevel,
  VasPerformance,
  Year,
  YearPeriodFromTo
}
import ru.auto.cabinet.dao.jdbc.{
  JdbcClientDao,
  JdbcKeyValueDao,
  JdbcVasPerformanceDao
}
import ru.auto.cabinet.environment._
import ru.auto.cabinet.model.moisha.Products._
import ru.auto.cabinet.service.moisha.MoishaClient
import ru.auto.cabinet.service.vos.{VosBadRequestException, VosClient}
import ru.auto.cabinet.service.moishaPoint

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class VasTaggingTaskSpec extends TaggingTaskSpec with VasTags {

  private val clientDao = mock[JdbcClientDao]
  private val vasPerformanceDao = mock[JdbcVasPerformanceDao]
  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient]
  private val moishaClient = mock[MoishaClient]

  private val task = new VasTaggingTask(
    clientDao,
    vasPerformanceDao,
    moishaClient,
    keyValueDao,
    vosClient) with TestInstrumented

  private val excludeTags = Seq(SpecialNeeded, FreshNeeded, PremiumNeeded)

  private val Mark1 = "VOLVO"
  private val Model1 = "XC90"
  private val Mark2 = "MERCEDES"
  private val Model2 = "300SL"
  private val Year1 = 2008
  private val GeobaseId1 = 1

  "VasTaggingTask" should "tag client offer" in {
    when(clientDao.getActiveClientIds)
      .thenReturn(futureClientIds(ClientId1, ClientId2, ClientId3))
    val offer1 = {
      val builder = ApiOfferModel.Offer.newBuilder()
      builder
        .setId(Offer1Id)
        .getCarInfoBuilder
        .setMark(Mark1)
        .setModel(Model1)
      builder.getDocumentsBuilder.setYear(Year1)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(GeobaseId1)
      builder.getAdditionalInfoBuilder.setCreationDate(Offer1Millis)
      builder.build()
    }
    val offer2 = {
      val builder = ApiOfferModel.Offer.newBuilder()
      builder
        .setId(Offer2Id)
        .getCarInfoBuilder
        .setMark(Mark2)
        .setModel(Model2)
      builder.getDocumentsBuilder.setYear(Year1)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(GeobaseId1)
      builder.getAdditionalInfoBuilder.setCreationDate(Offer2Millis)
      builder.build()
    }
    // Mockito doesn't know about Scala function's default arguments :(
    // this invocation is the same as vosClient.getOffers(ClientId1, after = LastTagging, excludeTags = excludeTags)
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        any(),
        argEq(LastTagging),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(Future(Seq(offer1)))
    when(
      vosClient.getOffers(
        argEq(ClientId2),
        any(),
        any(),
        argEq(LastTagging),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(Future(Seq.empty))
    when(
      vosClient.getOffers(
        argEq(ClientId3),
        any(),
        any(),
        argEq(LastTagging),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(Future(Seq(offer2)))
    when(
      vasPerformanceDao
        .find(new Mark(Mark1), new Model(Model1), new Year(Year1), Capital))
      .thenReturn(
        Future(
          Some(
            VasPerformance(
              new Mark(Mark1),
              new Model(Model1),
              YearPeriodFromTo(new Year(2005), new Year(2009)),
              regionType = Capital,
              premium = new RecommendationLevel(1000),
              special = new RecommendationLevel(490),
              fresh = new RecommendationLevel(50)
            )
          )))
    when(
      vasPerformanceDao
        .find(new Mark(Mark2), new Model(Model2), new Year(Year1), Capital))
      .thenReturn(
        Future.successful(None)
      )
    val from = startOfToday
    val to = endOfDay(from)
    when(moishaClient.getPrices(offer1, Seq(Premium, Special, Boost), from, to))
      .thenReturn(
        Future(
          Seq(
            moishaPoint(Premium, 999L),
            moishaPoint(Special, 50L),
            moishaPoint(Boost, 50L)
          )))
    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    when(vosClient.deleteTag(any(), any())(any())).thenReturn(futureUnit)
    val newLastTagging = task.doTagging(LastTagging).futureValue
    newLastTagging shouldBe Some(Offer1Odt)
    verify(vosClient).deleteTag(Offer1Id, PremiumNeeded)
    verify(vosClient).deleteTag(Offer1Id, SpecialNeeded)
    verify(vosClient).putTag(Offer1Id, FreshNeeded)
  }

  it should "not tag anything if all vas have zero efficiency" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    val offer1 = {
      val builder = ApiOfferModel.Offer.newBuilder()
      builder
        .setId(Offer1Id)
        .getCarInfoBuilder
        .setMark(Mark1)
        .setModel(Model1)
      builder.getDocumentsBuilder.setYear(Year1)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(GeobaseId1)
      builder.getAdditionalInfoBuilder.setCreationDate(Offer1Millis)
      builder.build()
    }
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        any(),
        argEq(LastTagging),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(Future(Seq(offer1)))
    when(
      vasPerformanceDao
        .find(new Mark(Mark1), new Model(Model1), new Year(Year1), Capital))
      .thenReturn(
        Future(
          Some(
            VasPerformance(
              new Mark(Mark1),
              new Model(Model1),
              YearPeriodFromTo(new Year(2005), new Year(2009)),
              regionType = Capital,
              premium = new RecommendationLevel(0),
              special = new RecommendationLevel(0),
              fresh = new RecommendationLevel(0)
            )
          )))
    task.doTagging(LastTagging).futureValue shouldBe LastTagging
  }

  it should "tag offer if only some of vas have zero efficiency" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    val offer1 = {
      val builder = ApiOfferModel.Offer.newBuilder()
      builder
        .setId(Offer1Id)
        .getCarInfoBuilder
        .setMark(Mark1)
        .setModel(Model1)
      builder.getDocumentsBuilder.setYear(Year1)
      builder.getSellerBuilder.getLocationBuilder.setGeobaseId(GeobaseId1)
      builder.getAdditionalInfoBuilder.setCreationDate(Offer1Millis)
      builder.build()
    }
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        any(),
        argEq(LastTagging),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(Future(Seq(offer1)))
    when(
      vasPerformanceDao
        .find(new Mark(Mark1), new Model(Model1), new Year(Year1), Capital))
      .thenReturn(
        Future(
          Some(
            VasPerformance(
              new Mark(Mark1),
              new Model(Model1),
              YearPeriodFromTo(new Year(2005), new Year(2009)),
              regionType = Capital,
              premium = new RecommendationLevel(100),
              special = new RecommendationLevel(0),
              fresh = new RecommendationLevel(0)
            )
          )))
    val from = startOfToday
    val to = endOfDay(from)
    when(moishaClient.getPrices(offer1, Seq(Premium, Special, Boost), from, to))
      .thenReturn(
        Future(
          Seq(
            moishaPoint(Premium, 999L),
            moishaPoint(Special, 50L),
            moishaPoint(Boost, 50L)
          )))
    when(vosClient.putTag(any(), any())(any())).thenReturn(futureUnit)
    when(vosClient.deleteTag(any(), any())(any())).thenReturn(futureUnit)
    val newLastTagging = task.doTagging(LastTagging).futureValue
    newLastTagging shouldBe Some(Offer1Odt)
    verify(vosClient).putTag(Offer1Id, PremiumNeeded)
    verify(vosClient).deleteTag(Offer1Id, FreshNeeded)
    verify(vosClient).deleteTag(Offer1Id, SpecialNeeded)
  }

  it should "return bad request exception if vos responds with 400" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(any(), any(), any(), any(), any(), any(), any())(
        any()))
      .thenReturn(
        Future.failed(new VosBadRequestException("Bad request"))
      )
    val result = Try(task.doTagging(None).futureValue)
    result.failed.get.getCause shouldBe a[VosBadRequestException]
  }
}
