package ru.yandex.vertis.billing.receipt

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.billing.receipt.BalanceReceiptImplSpec.MockHttpResponder

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class BalanceReceiptImplSpec
  extends TestKit(ActorSystem("BalanceReceiptImplSpec"))
  with Matchers
  with AnyWordSpecLike
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ScalaFutures {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(500, Millis))

  override protected def beforeEach(): Unit = {
    responder.reset()
    super.beforeEach()
  }

  import ExecutionContext.Implicits.global

  implicit val materializer: Materializer = Materializer.matFromSystem

  implicit val timeout: FiniteDuration = FiniteDuration(3, "seconds")

  val responder = new MockHttpResponder()

  val provider = new StaticTokenProvider(whitespiritToken = "ws", darkspiritToken = "ds", rendererToken = "r")

  val client = new BalanceReceiptImpl("", "", "", provider, responder)

  "BalanceReceiptImplSpec" should {
    "use whitespirit token in 'receipt'" in {
      responder.expectHeaders(BalanceReceiptImpl.`X-Ya-Service-Ticket`("ws"))

      client.receipt(content).futureValue
    }

    "use darkspirit token in 'commit'" in {
      responder.expectHeaders(BalanceReceiptImpl.`X-Ya-Service-Ticket`("ds"))

      client.commit(Array.emptyByteArray).futureValue
    }

    "use renderer token in 'render'" in {
      responder.expectHeaders(BalanceReceiptImpl.`X-Ya-Service-Ticket`("r"))

      client.render(Array.emptyByteArray).futureValue
    }
  }

}

object BalanceReceiptImplSpec {

  class MockHttpResponder(implicit mat: Materializer, timeout: FiniteDuration, ec: ExecutionContext)
    extends (HttpRequest => Future[HttpResponse]) {

    private var expectedHeaders: ArrayBuffer[HttpHeader] = ArrayBuffer.empty

    def reset(): Unit = {
      expectedHeaders = ArrayBuffer.empty
    }

    def expectHeaders(headers: HttpHeader*): Unit = {
      expectedHeaders ++= headers: Unit
    }

    override def apply(req: HttpRequest): Future[HttpResponse] =
      for {
        _ <- Future {
          expectedHeaders.foreach { h =>
            require(req.headers.contains(h), s"Expected header [$h], but got [${req.headers.mkString(", ")}]")
          }
        }
      } yield HttpResponse(StatusCodes.OK)
  }
}
