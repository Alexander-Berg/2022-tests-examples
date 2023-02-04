package ru.yandex.complaints.api.directives.api.schedule

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.junit.JUnitRunner
import ru.yandex.complaints.api.directives.DirectiveSpec
import spray.http.Uri
import spray.routing.directives.BasicDirectives

/**
  * Created by s-reznick on 21.03.17.
  */
@RunWith(classOf[JUnitRunner])
class ScheduleDirectiveSpec
  extends WordSpec
  with Matchers
  with DirectiveSpec with BasicDirectives {

  def createMap(renotify: Option[String]): Map[String, String] = {
    normaizeMap(Seq(
      ScheduleDirective.Renotify -> renotify
    ))
  }

  val OfferId = "offer_131432532"

  def offerPath(s: String) = Uri.Path / s

  val GoodRequests =
    for {
      toRenotify <- withNone(GoodFlags)
    } yield createMap(toRenotify)

  "ScheduleDirective" should {
    "accept correct values" in {
      for (params <- GoodRequests) {
        val res = check(ScheduleDirective.instance, params,
          path = offerPath(OfferId))
        assert(res.isAccepted)
        assert(res.rejectReasons.isEmpty)
      }
    }

    "reject incorrect renotify flag" in {
      for (params <- withParam(GoodRequests,
        ScheduleDirective.Renotify, BadFlags)) {
        val res = check(ScheduleDirective.instance, params = params, path = offerPath(OfferId))
        assert(!res.isAccepted)
        assert(res.rejectReasons.size == 1)
      }
    }
  }
}