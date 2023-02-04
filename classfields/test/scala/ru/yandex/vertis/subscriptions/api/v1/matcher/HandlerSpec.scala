package ru.yandex.vertis.subscriptions.api.v1.matcher

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import akka.testkit.{TestActorRef, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.subscriptions.Model.Document
import ru.yandex.vertis.subscriptions.api.RouteTestWithConfig
import ru.yandex.vertis.subscriptions.backend.matching.{MatcherUpdaterActor, ScalaMatcherActor}
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import spray.http._
import spray.routing.HttpService

import scala.concurrent.duration._

/**
  * Spec on [[ru.yandex.vertis.subscriptions.api.v1.matcher.Handler]]
  */
@RunWith(classOf[JUnitRunner])
class HandlerSpec extends Matchers with WordSpecLike with RouteTestWithConfig with HttpService {

  val autoMatcher = TestProbe()
  val realtyMatcher = TestProbe()

  private val matcherActors = Map(
    "auto" -> autoMatcher.ref,
    "realty" -> realtyMatcher.ref
  )

  val autoUpdater = TestProbe()
  val realtyUpdater = TestProbe()

  private val matcherUpdaters = Map(
    "auto" -> autoUpdater.ref,
    "realty" -> realtyUpdater.ref
  )

  /** Route of handler under test */
  private val route = seal {
    TestActorRef(new ServiceThrottledHandler {
      protected def baseMatchersCount: Int = 4

      protected def backendMatcherActor(service: String) = matcherActors.get(service)

      protected def backendMatcherUpdaterActor(service: String) = matcherUpdaters.get(service)
    }).underlyingActor.route
  }

  implicit val routeTestTimeout = RouteTestTimeout(3.seconds)

  "POST /auto" should {
    "produce match request to backend auto matcher" in {
      val docs = CoreGenerators.documents.next(10)
      val entity = getDocumentsEntity(docs)
      Post("/auto", HttpEntity(service.Handler.DocumentsContentType, entity))
        .withHeaders(HttpHeaders.`Content-Encoding`(HttpEncodings.gzip)) ~>
        route
      autoMatcher.expectMsgPF(10.second) {
        case r: ScalaMatcherActor.Request => true
        case _ => false
      }
      realtyMatcher.expectNoMsg(1.second)
    }
  }

  "POST /market" should {
    "respond 404" in {
      val docs = CoreGenerators.documents.next(10)
      val entity = getDocumentsEntity(docs)
      Post("/market", HttpEntity(service.Handler.DocumentsContentType, entity))
        .withHeaders(HttpHeaders.`Content-Encoding`(HttpEncodings.gzip)) ~>
        sealRoute(route) ~> check {
        response.status should be(StatusCodes.NotFound)
      }
      autoMatcher.expectNoMsg(1.second)
      realtyMatcher.expectNoMsg(1.second)
    }
  }

  "GET /auto" should {
    "produce get request to matcher updater" in {
      Get("/auto/foo") ~> route
      autoUpdater.expectMsgPF() {
        case _: MatcherUpdaterActor.GetSubscriptionQuery => true
      }
    }

    "produce find request to matcher updater" in {
      Get("/auto/find/foo") ~> route
      autoUpdater.expectMsgPF() {
        case _: MatcherUpdaterActor.FindSubscription => true
      }
    }
  }

  private def getDocumentsEntity(docs: Iterable[Document]) = {
    val bytes = new ByteArrayOutputStream(1024)
    val out = new GZIPOutputStream(bytes)
    for (d <- docs)
      d.writeDelimitedTo(out)
    out.close()
    bytes.toByteArray
  }

  override def cleanUp(): Unit = {
    system.terminate()
  }

  implicit def actorRefFactory = system
}
