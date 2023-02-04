package ru.auto.api.managers.lenta

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.SuccessResponse
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.SubscriptionNotFoundException
import ru.auto.api.managers.lenta.LentaManagerSpec.{makeResultQuery, makeSubscription}
import ru.auto.api.model.{AutoruUser, RequestParams}
import ru.auto.api.services.lenta.LentaClient
import ru.auto.api.services.lenta.LentaClient.{FeedCache, UserDataCache}
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.lenta.ApiModel.{Subscription => OuterSubscription}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.Model.Query.{OrQuery, TermQuery}
import ru.yandex.vertis.subscriptions.Model.Term.Point
import ru.yandex.vertis.subscriptions.Model.{Query, RequestSource, Term}
import ru.yandex.vertis.subscriptions.api.ApiModel.Subscription
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class LentaManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {
  val lentaClient: LentaClient = mock[LentaClient]
  val subscriptionsManager: LentaSubscriptionsManager = mock[LentaSubscriptionsManager]

  val lentaManager: LentaManager = new LentaManager(lentaClient, subscriptionsManager)

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r
  }

  before {
    reset(lentaClient, subscriptionsManager)
  }

  "LentaManager" should {
    "set subscription" in {

      val user = AutoruUser(123)
      val subscription = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Iterable("tag1", "tag2", "tag3").asJava)
        .build()
      val resultQuery = makeResultQuery(subscription)
      val successResponse = SuccessResponse.newBuilder().build()
      val innerSubscription = Subscription.newBuilder().build()

      when(subscriptionsManager.set(?, ?)(?)).thenReturnF(innerSubscription)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

      val res = lentaManager.setSubscription(user, subscription).futureValue

      verify(subscriptionsManager).set(user, resultQuery)(request)
      verify(lentaClient).invalidateCache(FeedCache, user)(request)
      verify(lentaClient).invalidateCache(UserDataCache, user)(request)

      res.getUserId shouldEqual user.toPlain
      res.getSubscription shouldEqual subscription
    }

    "LentaManager" should {
      "delete subscription" in {

        val user = AutoruUser(123)
        val successResponse = SuccessResponse.newBuilder().build()

        when(subscriptionsManager.delete(?)(?)).thenReturnF(successResponse)
        when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
        when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

        val res = lentaManager.deleteSubscription(user).futureValue

        verify(subscriptionsManager).delete(user)(request)
        verify(lentaClient).invalidateCache(FeedCache, user)(request)
        verify(lentaClient).invalidateCache(UserDataCache, user)(request)

        res shouldEqual successResponse
      }
    }

    "LentaManager" should {
      "get subscription" in {

        val user = AutoruUser(123)
        val subscription = OuterSubscription
          .newBuilder()
          .addAllIncludeTags(Iterable("tag1", "tag2", "tag3").asJava)
          .build()
        val innerSubscription = makeSubscription(subscription)

        when(subscriptionsManager.get(?)(?)).thenReturnF(Seq(innerSubscription))

        val res = lentaManager.getUserSubscription(user).futureValue

        verify(subscriptionsManager).get(user)(request)
        verifyNoMoreInteractions(lentaClient)

        res.getIncludeTagsList shouldEqual subscription.getIncludeTagsList
        res.getExcludeTagsList.isEmpty shouldEqual true
      }
    }
  }

  "LentaManager" should {
    "add subscription tags" in {

      val user = AutoruUser(123)
      val subscription1 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3").asJava)
        .build()
      val resultQuery1 = makeResultQuery(subscription1)
      val successResponse = SuccessResponse.newBuilder().build()
      val innerSubscription1 = makeSubscription(subscription1)

      when(subscriptionsManager.get(?)(?)).thenReturnF(Seq(innerSubscription1))
      when(subscriptionsManager.set(?, ?)(?)).thenReturnF(innerSubscription1)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

      val res1 = lentaManager.setSubscription(user, subscription1).futureValue

      res1.getUserId shouldEqual user.toPlain
      res1.getSubscription shouldEqual subscription1

      val subscription2 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag4", "tag5").asJava)
        .build()
      val resultQuery2 = makeResultQuery(subscription2)
      val wholeSubscription = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3", "tag4", "tag5").asJava)
        .build()
      val resultWholeQuery = makeResultQuery(wholeSubscription)

      val res2 = lentaManager.addSubscriptionTags(user, subscription2).futureValue

      val argument = ArgumentCaptor.forClass(classOf[Query])

      verify(subscriptionsManager).get(user)(request)
      verify(subscriptionsManager, times(2)).set(eq(user), argument.capture())(eq(request))

      verify(lentaClient, times(2)).invalidateCache(FeedCache, user)(request)
      verify(lentaClient, times(2)).invalidateCache(UserDataCache, user)(request)

      argument.getAllValues.get(0).asInstanceOf[Query] shouldEqual resultQuery1
      val actualTags = argument.getAllValues
        .get(1)
        .asInstanceOf[Query]
        .getOr
        .getQueriesList
        .asScala
        .map(_.getTerm.getTerm.getPoint.getValue)
        .sorted
      val expectedTags = resultWholeQuery.getOr.getQueriesList.asScala.map(_.getTerm.getTerm.getPoint.getValue).sorted
      actualTags shouldEqual expectedTags
    }
  }

  "LentaManager" should {
    "delete subscription tags" in {

      val user = AutoruUser(123)
      val subscription1 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3").asJava)
        .build()
      val resultQuery1 = makeResultQuery(subscription1)
      val successResponse = SuccessResponse.newBuilder().build()
      val innerSubscription1 = makeSubscription(subscription1)

      when(subscriptionsManager.get(?)(?)).thenReturnF(Seq(innerSubscription1))
      when(subscriptionsManager.set(?, ?)(?)).thenReturnF(innerSubscription1)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

      val res1 = lentaManager.setSubscription(user, subscription1).futureValue

      res1.getUserId shouldEqual user.toPlain
      res1.getSubscription shouldEqual subscription1

      val subscription2 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag2").asJava)
        .build()
      val resultQuery2 = makeResultQuery(subscription2)
      val wholeSubscription = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag3").asJava)
        .build()
      val resultWholeQuery = makeResultQuery(wholeSubscription)

      val res2 = lentaManager.deleteSubscriptionTags(user, subscription2).futureValue

      val argument = ArgumentCaptor.forClass(classOf[Query])

      verify(subscriptionsManager).get(user)(request)
      verify(subscriptionsManager, times(2)).set(eq(user), argument.capture())(eq(request))

      verify(lentaClient, times(2)).invalidateCache(FeedCache, user)(request)
      verify(lentaClient, times(2)).invalidateCache(UserDataCache, user)(request)

      argument.getAllValues.get(0).asInstanceOf[Query] shouldEqual resultQuery1
      val actualTags = argument.getAllValues
        .get(1)
        .asInstanceOf[Query]
        .getOr
        .getQueriesList
        .asScala
        .map(_.getTerm.getTerm.getPoint.getValue)
        .sorted
      val expectedTags = resultWholeQuery.getOr.getQueriesList.asScala.map(_.getTerm.getTerm.getPoint.getValue).sorted
      actualTags shouldEqual expectedTags
    }
  }

  "LentaManager" should {
    "delete all subscription tags" in {

      val user = AutoruUser(123)
      val subscription1 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3").asJava)
        .build()
      val resultQuery1 = makeResultQuery(subscription1)
      val successResponse = SuccessResponse.newBuilder().build()
      val innerSubscription1 = makeSubscription(subscription1)

      when(subscriptionsManager.get(?)(?)).thenReturnF(Seq(innerSubscription1))
      when(subscriptionsManager.set(?, ?)(?)).thenReturnF(innerSubscription1)
      when(subscriptionsManager.delete(?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

      val res1 = lentaManager.setSubscription(user, subscription1).futureValue

      res1.getUserId shouldEqual user.toPlain
      res1.getSubscription shouldEqual subscription1

      val subscription2 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3").asJava)
        .build()

      val res2 = lentaManager.deleteSubscriptionTags(user, subscription2).futureValue
      res2.getUserId shouldEqual "user:123"
      res2.hasSubscription shouldEqual false

      val argument = ArgumentCaptor.forClass(classOf[Query])

      verify(subscriptionsManager).get(user)(request)
      verify(subscriptionsManager).delete(user)(request)
      verify(subscriptionsManager, times(1)).set(eq(user), argument.capture())(eq(request))

      verify(lentaClient, times(2)).invalidateCache(FeedCache, user)(request)
      verify(lentaClient, times(2)).invalidateCache(UserDataCache, user)(request)

      argument.getAllValues.get(0).asInstanceOf[Query] shouldEqual resultQuery1
    }
  }

  "LentaManager" should {
    "not fail if deleting tags of empty subscription" in {

      val user = AutoruUser(123)
      val successResponse = SuccessResponse.newBuilder().build()
      val failedResponse = new SubscriptionNotFoundException(Some(""))

      when(subscriptionsManager.get(?)(?)).thenThrowF(failedResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)
      when(lentaClient.invalidateCache(?, ?)(?)).thenReturnF(successResponse)

      val subscription2 = OuterSubscription
        .newBuilder()
        .addAllIncludeTags(Set("tag1", "tag2", "tag3").asJava)
        .build()

      val res2 = lentaManager.deleteSubscriptionTags(user, subscription2).futureValue
      res2.getUserId shouldEqual "user:123"
      res2.hasSubscription shouldEqual false
    }
  }

}

object LentaManagerSpec {

  private def makeQuery(tag: String): Query = {
    Query
      .newBuilder()
      .setTerm(
        TermQuery
          .newBuilder()
          .setTerm(
            Term
              .newBuilder()
              .setName("tag")
              .setPoint(
                Point
                  .newBuilder()
                  .setValue(tag)
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
  }

  def makeSubscription(subscription: OuterSubscription): Subscription = {
    Subscription
      .newBuilder()
      .setRequest(
        RequestSource
          .newBuilder()
          .setQuery(
            Query
              .newBuilder()
              .setOr(
                OrQuery
                  .newBuilder()
                  .addAllQueries(subscription.getIncludeTagsList.asScala.map(tag => makeQuery(tag)).asJava)
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
  }

  def makeResultQuery(subscription1: OuterSubscription): Query = {
    Query
      .newBuilder()
      .setOr(
        OrQuery
          .newBuilder()
          .addAllQueries(subscription1.getIncludeTagsList.asScala.map(tag => makeQuery(tag)).asJava)
          .build()
      )
      .build()
  }

}
