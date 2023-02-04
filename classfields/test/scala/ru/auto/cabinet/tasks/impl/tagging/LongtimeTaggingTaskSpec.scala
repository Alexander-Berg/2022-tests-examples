package ru.auto.cabinet.tasks.impl.tagging

import org.mockito.ArgumentMatchers.{eq => argEq, _}
import org.mockito.Mockito._
import org.scalatest.exceptions.TestFailedException
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.cabinet.dao.jdbc.{JdbcClientDao, JdbcKeyValueDao}
import ru.auto.cabinet.environment.startOfToday
import ru.auto.cabinet.service.vos.{
  VosBadRequestException,
  VosClient,
  VosException
}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class LongtimeTaggingTaskSpec extends TaggingTaskSpec {
  private val clientDao = mock[JdbcClientDao]
  private val keyValueDao = mock[JdbcKeyValueDao]
  private val vosClient = mock[VosClient](RETURNS_SMART_NULLS)

  private val LongTime = LastTagging

  private val longtimePeriodDays = {
    var curr = LongTime.get

    def nextDay() = {
      val result = curr
      curr = curr.plusDays(1)
      result
    }

    LazyList.continually(nextDay()).takeWhile(_.isBefore(startOfToday)).size
  }

  private val task = new LongtimeTaggingTask(
    clientDao,
    keyValueDao,
    vosClient,
    longtimePeriodDays) with TestInstrumented

  import task.tag
  private val excludeTags = Seq(tag)

  "LongtimeTaggingTask" should "tag client offer" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    // Mockito doesn't know about Scala function's default arguments :(
    // this invocation is the same as vosClient.getOffers(ClientId1, after = LongTime, excludeTags = excludeTags)
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(Seq(futureOffer(Offer1Id, Offer1Millis)))
    )
    when(vosClient.putTag(Offer1Id, tag)).thenReturn(futureUnit)
    task.doTagging(None).futureValue shouldBe None
    verify(clientDao).getActiveClientIds
    // seems like mockito don't know about scala method overloads, that's why
    // there is no verifying of getOffers() method invocations in this and other tests
    verify(vosClient).putTag(Offer1Id, tag)
    verifyNoMoreInteractions(clientDao)
  }

  it should "tag nothing if no active clients" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds())
    task.doTagging(None).futureValue shouldBe None
    verify(clientDao).getActiveClientIds
    verifyNoMoreInteractions(clientDao)
    verifyNoInteractions(vosClient)
  }

  it should "tag nothing if no active offers" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.successful(Seq())
    )
    task.doTagging(None).futureValue shouldBe None
    verify(clientDao).getActiveClientIds
    verify(vosClient, times(0)).putTag(any(), any())(any())
    verifyNoMoreInteractions(clientDao)
  }

  it should "tag nothing if offer already with tag" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future(
        Seq()
      ) // futureOffer(Offer1Id, Offer1Millis, tag))) is filtered out by excludeTag param
    )
    task.doTagging(None).futureValue shouldBe None
    verify(clientDao).getActiveClientIds
    verify(vosClient, times(0)).putTag(any(), any())(any())
    verifyNoMoreInteractions(clientDao)
  }

  it should "throw error if put tagging failed" in {
    when(clientDao.getActiveClientIds).thenReturn(futureClientIds(ClientId1))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(Seq(futureOffer(Offer1Id, Offer1Millis)))
    )
    when(vosClient.putTag(Offer1Id, tag)).thenReturn(
      Future.failed(new VosException("Tagging failed"))
    )
    try {
      task.doTagging(None).futureValue
      throw new RuntimeException
    } catch {
      case e: TestFailedException =>
        e.getCause shouldBe a[VosException]
    }
    verify(clientDao).getActiveClientIds
    verify(vosClient).putTag(Offer1Id, tag)
    verifyNoMoreInteractions(clientDao)
  }

  it should "handle properly multiple clients and offers" in {
    when(clientDao.getActiveClientIds).thenReturn(
      futureClientIds(
        ClientId1,
        ClientId2,
        ClientId3,
        ClientId4
      ))
    when(
      vosClient.getOffers(
        argEq(ClientId1),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(
        Seq(
          futureOffer(Offer1Id, Offer1Millis)
        ))
    )
    when(
      vosClient.getOffers(
        argEq(ClientId2),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(
        Seq[Future[Offer]](
          // futureOffer(Offer2Id, Offer2Millis, tag) is filtered out by excludeTag param
        ))
    )
    when(
      vosClient.getOffers(
        argEq(ClientId3),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(
        Seq(
          futureOffer(Offer3Id, Offer3Millis),
          futureOffer(Offer4Id, Offer4Millis)
        ))
    )
    when(
      vosClient.getOffers(
        argEq(ClientId4),
        any(),
        argEq(LongTime),
        any(),
        any(),
        argEq(excludeTags),
        any())(any())).thenReturn(
      Future.sequence(
        Seq(
          futureOffer(Offer5Id, Offer5Millis, "some-random-tag"),
          futureOffer(Offer6Id, Offer6Millis)
        ))
    )
    when(vosClient.putTag(Offer1Id, tag)).thenReturn(futureUnit)
    when(vosClient.putTag(Offer2Id, tag)).thenReturn(futureUnit)
    when(vosClient.putTag(Offer3Id, tag)).thenReturn(futureUnit)
    when(vosClient.putTag(Offer4Id, tag)).thenReturn(futureUnit)
    when(vosClient.putTag(Offer5Id, tag)).thenReturn(futureUnit)
    when(vosClient.putTag(Offer6Id, tag)).thenReturn(futureUnit)
    task.doTagging(None).futureValue shouldBe None
    verify(clientDao).getActiveClientIds
    verify(vosClient, times(5)).putTag(any(), argEq(tag))(any())
    verifyNoMoreInteractions(clientDao)
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
