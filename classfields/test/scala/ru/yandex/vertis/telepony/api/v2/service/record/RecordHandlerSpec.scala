package ru.yandex.vertis.telepony.api.v2.service.record

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{`Content-Disposition`, ContentDispositionTypes}
import akka.http.scaladsl.server.Route
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.service.RecordServiceProbe
import ru.yandex.vertis.telepony.exception.RecordNotFoundException
import ru.yandex.vertis.telepony.model.{OperatorAccounts, Record, RecordMeta}
import ru.yandex.vertis.telepony.service.{RecordService, TranscriptionTaskService}

class RecordHandlerSpec extends RouteTest {

  def handlerSuite: (Route, TestProbe) = {
    val probe = TestProbe()
    val tar = seal(
      new RecordHandler {
        override def recordService: RecordService =
          new RecordServiceProbe(probe.ref)
        override def transcriptionTaskService: TranscriptionTaskService = ???
      }.route
    )
    (tar, probe)
  }

  "RecordHandler" should {
    "provide records" in {
      val (handler, probe) = handlerSuite
      val op = OperatorAccounts.BillingRealty
      val data = Array.ofDim[Byte](10)
      util.Random.nextBytes(data)
      val filename = "baz.mp3"
      val record = Record(
        RecordMeta("1", op, "foo://bar", Some(filename), None, DateTime.now(), customS3Prefix = Some("prefix/")),
        data
      )
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("get", "1", _) =>
            sender ! record
            this
        }
      })
      Get("/1") ~> handler ~> check {
        val contentDisposition = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))
        response.status should ===(StatusCodes.OK)
        response.headers should contain(contentDisposition)
      }
    }
    "return 404 when there is no record" in {
      val (handler, probe) = handlerSuite
      val op = OperatorAccounts.BillingRealty
      val data = Array.ofDim[Byte](10)
      util.Random.nextBytes(data)
      val filename = "baz.mp3"
      val record =
        Record(RecordMeta("1", op, "foo://bar", Some(filename), None, DateTime.now(), customS3Prefix = None), data)
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("get", "1", _) =>
            sender ! Failure(RecordNotFoundException(record.meta.id))
            this
        }
      })
      Get("/1") ~> handler ~> check {
        response.status should ===(StatusCodes.NotFound)
      }
    }
  }
}
