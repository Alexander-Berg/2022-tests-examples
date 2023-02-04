package ru.yandex.vertis.telepony.api.v2.service.number

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import org.slf4j.LoggerFactory
import ru.yandex.vertis.telepony.api.DomainExceptionHandler._
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.service.{OperatorNumberServiceProbeV2, RedirectServiceProbeV2}
import ru.yandex.vertis.telepony.api.v2.view.json.number.{OperatorNumberView, UpdateRequestView}
import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2.Filter
import ru.yandex.vertis.telepony.exception.{ResourceChangedException, ResourceLockedException}
import ru.yandex.vertis.telepony.generator.Generator.PhoneGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.json.JsonViewCompanion
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.{CreateRequest, UpdateRequestV2}
import ru.yandex.vertis.telepony.service.RedirectServiceV2.ComplainRequest
import ru.yandex.vertis.telepony.service.{OperatorNumberServiceV2, RedirectServiceV2}
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{OperatorContext, Page, Uid}

import scala.concurrent.duration.DurationInt

/**
  * @author evans
  */
class NumberHandlerSpec extends RouteTest {

  private val phone = PhoneGen.next
  private val num = phone.value

  private val ocHeaders = RawHeader("X-Yandex-Operator-Uid", "123")
  private val page = Page(1, 2)
  private val Moscow = 213

  val sample =
    OperatorNumber(phone, OperatorAccounts.MttShared, Operators.Mtt, Moscow, PhoneTypes.Local, Status.New(None), None)

  val companion: JsonViewCompanion[OperatorNumberView, OperatorNumber] =
    OperatorNumberView
  val unit = ()

  def updateRequest: UpdateRequestV2 = UpdateRequestV2(Some(Moscow), None, None)

  implicit def m: ToEntityMarshaller[UpdateRequestV2] = UpdateRequestView.modelMarshaller

  def handlerSuite: (Route, TestProbe) = {
    val probe = TestProbe()
    val tar = seal(
      new NumberHandler {
        override protected def operatorNumberServiceV2: OperatorNumberServiceV2 =
          new OperatorNumberServiceProbeV2(probe.ref)

        override protected def redirectServiceV2: RedirectServiceV2 =
          new RedirectServiceProbeV2(probe.ref)
      }.route
    )
    (tar, probe)
  }

  "NumberHandler" should {
    "update number" in {
      val (handler, probe) = handlerSuite
      import OperatorNumberView._
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("update", n, req, _) if phone == n && req == updateRequest =>
            sender ! sample
            this
        }
      })
      Put(s"/$num", updateRequest).withHeaders(ocHeaders) ~> handler ~> check {
        responseAs[OperatorNumberView] shouldEqual asView(sample)
        status shouldEqual StatusCodes.OK
      }
    }
    "handle resource has been changed error" in {
      val (handler, probe) = handlerSuite
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("update", n, req, _) if phone == n && req == updateRequest =>
            val failure: Failure = Failure(ResourceChangedException(phone.toString))
            LoggerFactory.getLogger(this.getClass).info(failure.toString)
            sender ! failure
            this
        }
      })
      Put(s"/$num", updateRequest).withHeaders(ocHeaders) ~> handler ~> check {
        status shouldEqual StatusCodes.Conflict
      }
    }
    "handle resource has been already locked error" in {
      val (handler, probe) = handlerSuite
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("update", n, req, _) if phone == n && req == updateRequest =>
            sender ! Failure(ResourceLockedException(phone.toString))
            this
        }
      })
      Put(s"/$num", updateRequest).withHeaders(ocHeaders) ~> handler ~> check {
        status shouldEqual StatusCodes.Locked
      }
    }

    "get phone" in {
      val (handler, probe) = handlerSuite
      import OperatorNumberView._
      probe.setAutoPilot(new AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
          case ("get", p, _) if p == phone =>
            sender ! sample
            this
        }
      })
      Get(s"/$num") ~> handler ~> check {
        responseAs[OperatorNumberView] shouldEqual asView(sample)
        status shouldEqual StatusCodes.OK
      }
    }

    "complain on phone" when {
      "ttl is set and permanently is not set" in {
        val (handler, probe) = handlerSuite
        val ttl = 10.seconds
        probe.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
            case ("complain", `phone`, ComplainRequest.WithTtl(Some(`ttl`)), _) =>
              sender ! unit
              this
          }
        })
        val uri = Uri(s"/$num/complain").withQuery(Query("ttl" -> s"${ttl.toSeconds}"))
        Post(uri) ~> handler ~> check {
          status shouldEqual StatusCodes.OK
        }
      }

      "permanently is set to true" in {
        val (handler, probe) = handlerSuite
        probe.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
            case ("complain", `phone`, ComplainRequest.WithTtl(None), _) =>
              sender ! unit
              this
          }
        })
        val uri = Uri(s"/$num/complain").withQuery(Query("permanently" -> "true"))
        Post(uri) ~> handler ~> check {
          status shouldEqual StatusCodes.OK
        }
      }

      "permanently is set to false and ttl is not set" in {
        val (handler, probe) = handlerSuite
        probe.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = msg match {
            case ("complain", `phone`, ComplainRequest.UseDefaultTtl, _) =>
              sender ! unit
              this
          }
        })
        val uri = Uri(s"/$num/complain").withQuery(Query("permanently" -> "false"))
        Post(uri) ~> handler ~> check { // TODO: there was sealRoute(...)
          status should be(StatusCodes.OK)
        }
      }

      "invalid ttl value is provided" in {
        val (handler, _) = handlerSuite
        val uri = Uri(s"/$num/complain").withQuery(Query("ttl" -> "vasya"))
        Post(uri) ~> handler ~> check {
          status should be(StatusCodes.BadRequest)
        }
      }
    }
  }
}
