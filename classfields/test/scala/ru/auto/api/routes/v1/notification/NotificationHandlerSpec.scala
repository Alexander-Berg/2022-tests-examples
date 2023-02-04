package ru.auto.api.routes.v1.notification

import akka.http.scaladsl.model.StatusCodes.OK
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.managers.notifications.NotificationManager
import ru.auto.api.model.ModelGenerators._
import NotificationModelGenerators._
import org.mockito.Mockito.verify
import ru.auto.api.services.MockedClients
import ru.auto.api.services.notification.NotificationClient
import ru.auto.api.util.{ManagerUtils, ProtobufSupport}
import ru.auto.notification.ApiModel._
import ru.yandex.vertis.spamalot._

import scala.jdk.CollectionConverters._

class NotificationHandlerSpec extends ApiSpec with ProtobufSupport with MockedClients with ScalaCheckPropertyChecks {

  override lazy val notificationManager: NotificationManager = mock[NotificationManager]
  override lazy val notificationClient: NotificationClient = mock[NotificationClient]

  "GET /notification/unread-count" should {

    "respond with unread notifications count" in {
      forAll(SessionResultGen, unreadCountResponseGen) { (session, response) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.unreadCount(?))
          .thenReturnF(response)

        Get("/1.0/notification/unread-count") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[UnreadCountResponse] shouldBe response
            }
            verify(notificationManager).unreadCount(?)
            Mockito.reset(notificationManager)
          }
      }
    }
  }
  "GET /notification/mark-read" should {
    "respond OK with ids list" in {
      forAll(SessionResultGen, listIdsRequestGen) { (session, request) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.markRead(?)(?))
          .thenReturnF(ManagerUtils.SuccessResponse)
        Put("/1.0/notification/mark-read", request) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
            }
          }
        verify(notificationManager).markRead(eq(request.getIdList.asScala.toSeq))(?)
        Mockito.reset(notificationManager)
      }

    }

    "respond OK without ids list" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.markRead(?)(?))
          .thenReturnF(ManagerUtils.SuccessResponse)
        Put("/1.0/notification/mark-read", ListIdsRequest.newBuilder.build()) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
            }
            verify(notificationManager).markRead(eq(Seq.empty))(?)
            Mockito.reset(notificationManager)
          }
      }
    }
  }
  "GET /notification/mark-all-read" should {

    "respond OK with topic" in {
      forAll(SessionResultGen, readableString) { (session, topic) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.markAllRead(?)(?))
          .thenReturnF(ManagerUtils.SuccessResponse)
        Put(s"/1.0/notification/mark-all-read?topic=$topic") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
            }
            verify(notificationManager).markAllRead(eq(Some(topic)))(?)
            Mockito.reset(notificationManager)
          }
      }
    }

    "respond OK without topic" in {
      forAll(SessionResultGen) { session =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.markAllRead(?)(?))
          .thenReturnF(ManagerUtils.SuccessResponse)
        Put("/1.0/notification/mark-all-read") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
            }
            verify(notificationManager).markAllRead(eq(None))(?)
            Mockito.reset(notificationManager)
          }
      }
    }
  }

  "POST /notification/list" should {

    "respond OK with topic list" in {
      forAll(SessionResultGen, listTopicsRequestGen, bool, paginationGen, listResponseGen) {

        (session, topics, newOnly, pagination, response) =>
          when(passportClient.getSession(?)(?)).thenReturnF(session)
          when(notificationManager.list(?, ?, ?)(?))
            .thenReturnF(response)
          val tempDateTime = new DateTime(pagination.getLastTs.getSeconds * 1000)
          val lastTs = tempDateTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
          Post(
            s"/1.0/notification/list?new-only=$newOnly" +
              s"&id-gte=${pagination.getLastId.getValue}" +
              s"&after-ts=$lastTs" +
              s"&limit=${pagination.getLimit.getValue}",
            topics
          ) ~>
            addHeader("x-session-id", session.getSession.getId) ~>
            xAuthorizationHeader ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe OK
                responseAs[ListResponse] shouldBe response
              }
              verify(notificationManager).list(
                eq(topics.getTopicList.asScala.toSeq),
                eq(newOnly),
                eq(Some(pagination))
              )(?)
              Mockito.reset(notificationManager)
            }
      }
    }

    "respond OK without pagination elements and topics and empty string" in {
      val topics = ListTopicsRequest.newBuilder.build()
      forAll(SessionResultGen, bool, paginationGen, listResponseGen) { (session, newOnly, pagination, response) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.list(?, ?, ?)(?))
          .thenReturnF(response)
        val newPagination = Pagination.newBuilder().setLastId(pagination.getLastId).build
        Post(
          s"/1.0/notification/list?new-only=$newOnly" +
            s"&id-gte=${pagination.getLastId.getValue}" +
            s"&after-ts=${""}",
          topics
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[ListResponse] shouldBe response
            }
            verify(notificationManager).list(
              eq(topics.getTopicList.asScala.toSeq),
              eq(newOnly),
              eq(Some(newPagination))
            )(?)
            Mockito.reset(notificationManager)
          }
      }
    }
    "respond OK without pagination elements and topics" in {
      val topics = ListTopicsRequest.newBuilder.build()
      forAll(SessionResultGen, bool, paginationGen, listResponseGen) { (session, newOnly, pagination, response) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.list(?, ?, ?)(?))
          .thenReturnF(response)
        val newPagination = Pagination.newBuilder().setLastId(pagination.getLastId).build
        Post(
          s"/1.0/notification/list?new-only=$newOnly" +
            s"&id-gte=${pagination.getLastId.getValue}",
          topics
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[ListResponse] shouldBe response
            }
            verify(notificationManager).list(
              eq(topics.getTopicList.asScala.toSeq),
              eq(newOnly),
              eq(Some(newPagination))
            )(?)
            Mockito.reset(notificationManager)
          }
      }
    }
    "respond OK without pagination at all" in {
      val topics = ListTopicsRequest.newBuilder.build()
      forAll(SessionResultGen, bool, listResponseGen) { (session, newOnly, response) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(notificationManager.list(?, ?, ?)(?))
          .thenReturnF(response)
        Post(
          s"/1.0/notification/list?new-only=$newOnly",
          topics
        ) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[ListResponse] shouldBe response
            }
            verify(notificationManager).list(eq(topics.getTopicList.asScala.toSeq), eq(newOnly), eq(None))(?)
            Mockito.reset(notificationManager)
          }
      }
    }
  }
}
