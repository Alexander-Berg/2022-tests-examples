package ru.yandex.vertis.passport.api.v2.service.moderation

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.Domains
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{DomainDirectives, MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.{proto, EventsResult}
import ru.yandex.vertis.passport.model.proto.EventPayload.ImplCase
import ru.yandex.vertis.passport.service.events.EventsService.Filter
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.util.Page

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

class ModerationHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {

  val base = "/api/2.x/auto/moderation"

  val eventsLogPath = s"$base/events-log"

  val DefaultPage = Page(0, DomainDirectives.DefaultPageSize)

  val eventEnvelope = ModelGenerators.eventEnvelope(Domains.Auto)

  "EventsLog handler" should {
    "provide events" in {
      when(eventsService.list(eq(Filter()), eq(DefaultPage))(?))
        .thenReturn(Future.successful(EventsResult(eventEnvelope.next(10).toList, 15)))
      Get(s"$eventsLogPath") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val result = responseAs[proto.EventsResult]
          result.getEventsList.size shouldBe 10
          result.getTotalCount shouldBe 15
        }
    }

    "apply paging" in {
      when(eventsService.list(eq(Filter()), eq(Page(3, 123)))(?))
        .thenReturn(Future.successful(EventsResult(Seq.empty, 0)))
      Get(
        Uri(s"$eventsLogPath")
          .withQuery(Uri.Query("pageNum" -> "3", "pageSize" -> "123"))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }

    "apply user ID" in {
      when(
        eventsService.list(eq(Filter(userId = Some("234"))), eq(DefaultPage))(?)
      ).thenReturn(Future.successful(EventsResult(Seq.empty, 0)))
      Get(
        Uri(s"$eventsLogPath")
          .withQuery(Uri.Query("user-id" -> "234"))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }

    "apply event type" in {
      when(eventsService.list(eq(Filter(eventType = Some(ImplCase.USER_CREATED))), eq(DefaultPage))(?))
        .thenReturn(Future.successful(EventsResult(Seq.empty, 0)))
      Get(
        Uri(s"$eventsLogPath")
          .withQuery(Uri.Query("event-type" -> "USER_CREATED"))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }

    "apply time ranges" in {
      val fromStr = "2015-01-30T17:00:00.000+03:00"
      val from = new DateTime(2015, 1, 30, 17, 0, 0, DateTimeZone.forOffsetHours(3))

      val toStr = "2017-01-30T17:00:00.000+03:00"
      val to = new DateTime(2017, 1, 30, 17, 0, 0, DateTimeZone.forOffsetHours(3))

      val filterCaptor: ArgumentCaptor[Filter] = ArgumentCaptor.forClass(classOf[Filter])
      when(eventsService.list(filterCaptor.capture(), eq(DefaultPage))(?))
        .thenReturn(Future.successful(EventsResult(Seq.empty, 0)))
      Get(
        Uri(s"$eventsLogPath")
          .withQuery(Uri.Query("datetime-from" -> fromStr, "datetime-to" -> toStr))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val filter = filterCaptor.getValue
          filter.from.get.getMillis shouldBe from.getMillis
          filter.to.get.getMillis shouldBe to.getMillis
        }
    }
  }
}
