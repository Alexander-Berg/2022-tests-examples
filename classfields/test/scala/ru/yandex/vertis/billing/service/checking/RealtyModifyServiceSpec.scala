package ru.yandex.vertis.billing.service.checking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{EventGen, Producer}
import ru.yandex.vertis.billing.model_core.{Division, Event}
import ru.yandex.vertis.billing.service.EventStoreService

import scala.util.{Success, Try}

/**
  * Spec on [[RealtyModifyService]]
  *
  * @author ruslansd
  */
class RealtyModifyServiceSpec extends AnyWordSpec with Matchers {

  private val regularEvents = EventGen.next(10).toList

  private val realtyPhoneClick = EventGen
    .next(5)
    .toList
    .map(e => e.copy(division = e.division.copy(project = "realty-api", component = "phone_click")))

  private val realtyPhoneShow = EventGen
    .next(5)
    .toList
    .map(e => e.copy(division = e.division.copy(project = "realty-api", component = "phone_show"))) ++
    EventGen.next(5).map(e => e.copy(division = e.division.copy(project = "realty", component = "phone_show")))

  "RealtyModifyService" should {

    "accept empty data" in {
      val service = EventStoreServiceImpl(events => ())
      service.store(Iterable()) match {
        case Success(()) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "modify realty phone click events" in {
      val service = EventStoreServiceImpl(events => {
        events.size should be(realtyPhoneClick.size)
        events.foreach(e => {
          e.division.project shouldBe Division.Projects.Realty.toString
          e.division.component shouldBe Division.Components.PhoneShow.toString
        })
      })

      service.store(realtyPhoneClick) match {
        case Success(()) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "modify realty phone show if need" in {
      val service = EventStoreServiceImpl(events => {
        events.size should be(realtyPhoneShow.size)
        events.foreach(e => {
          e.division.project shouldBe Division.Projects.Realty.toString
          e.division.component shouldBe Division.Components.PhoneShow.toString
        })
      })

      service.store(realtyPhoneShow) match {
        case Success(()) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "not modify other events" in {
      val service = EventStoreServiceImpl(events => {
        val sources = regularEvents
        val results = events.toSeq
        results.size should be(sources.size)
        sources
          .zip(results)
          .foreach { case (s, r) =>
            s should be(r)
          }
      })

      service.store(regularEvents) match {
        case Success(()) =>
        case other => fail(s"Unexpected $other")
      }
    }

  }

}

class EventStoreServiceImpl(f: Iterable[Event] => Unit) extends EventStoreService {

  override def store(events: Iterable[Event]): Try[Unit] =
    Try(f(events))
}

object EventStoreServiceImpl {

  def apply(f: Iterable[Event] => Unit): EventStoreService =
    new EventStoreServiceImpl(f) with RealtyModifyService
}
