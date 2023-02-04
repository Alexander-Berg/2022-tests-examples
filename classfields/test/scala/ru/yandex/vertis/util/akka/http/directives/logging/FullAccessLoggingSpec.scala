package ru.yandex.vertis.util.akka.http.directives.logging


import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, ScalatestRouteTest}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import ch.qos.logback.classic
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.EncoderBase
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.{Logger, LoggerFactory}
import ru.yandex.vertis.util.akka.http.directives.logging.FullAccessLoggingSpec.FakeLogging

import scala.collection.immutable
import scala.concurrent.duration.DurationInt

/**
  * @author @logab
  */
class FullAccessLoggingSpec
    extends WordSpecLike
        with Matchers
        with RouteTest
        with ScalatestRouteTest
        with Eventually
        with ScalaFutures
        with TableDrivenPropertyChecks {

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(3.seconds, 50.millis)

  class TestLoggingDirective(request: HttpRequest, httpResponse: HttpResponse)
      extends server.FullTruncatedLogging {

    val hookedLogger = new FakeLogging("FullAccessLoggingSpec")

    override protected def accessLog: Logger = hookedLogger.fakeLogger

    override protected def requestId(request: HttpRequest): String = request.hashCode().toString

    val route: Route = fullLogging {_.complete(httpResponse)}

    val resp: HttpResponse = {
      val result: RouteTestResult = request ~> route
      var resp: HttpResponse = null
      result ~> check {resp = response}
      eventually {hookedLogger.logOutput.size shouldBe 2}
      resp
    }

    def run(): Unit = {
      checkLog(request, hookedLogger.logOutput.head)
      checkLog(resp, hookedLogger.logOutput.tail.head)
    }

    def checkLog(message: HttpMessage, actual: String): Unit = {
      actual should startWith(requestId(request))
      message
          .headers
          .filter { header =>
            message.isRequest() && header.renderInRequests() ||
                message.isResponse() && header.renderInResponses()
          }
          .foreach { header =>
            actual should include(header.toString())
          }
      if (message.isRequest()) {
        checkRequest(message.asInstanceOf[HttpRequest], actual)
      } else {
        checkResponse(message.asInstanceOf[HttpResponse], actual)
      }
    }

    def checkRequest(request: HttpRequest, actual: String): Unit = {
      actual should include(">>>")
      checkMessage(request, actual)
    }

    def checkMessage(httpMessage: HttpMessage, actual: String): Unit =
      actual should include(entityToString(httpMessage.entity()))

    def checkResponse(resp: HttpResponse, actual: String): Unit = {
      actual should include("<<<")
      checkMessage(resp, actual)
    }
  }

  import MediaTypes._
  import akka.http.scaladsl.model.headers._

  val url = "http://localhost:12345/"
  val requests = List(
    Get(url).withHeaders(`Content-Type`(`application/json`)),
    Get(url),
    Post(url, "Nonempty text"),
    Post(url, HttpEntity(
      ContentType(`multipart/form-data`),
      "Chunked".getBytes().length,
      Source.single(ByteString("Chunked")))),
    Post(url, HttpEntity("Binary".getBytes)))

  val base = HttpResponse(
    StatusCodes.OK,
    immutable.Seq[HttpHeader](RawHeader("X-request-id", "1")),
    HttpEntity(ContentType(`text/plain`, `UTF-8`), ""),
    HttpProtocols.`HTTP/1.1`)

  val responses = List(
    base.copy(entity = HttpEntity(
      ContentType(`text/plain`, `UTF-8`), "")),
    base.copy(entity = HttpEntity(
      ContentType(`text/plain`, `UTF-8`), "Text")),
    base.copy(entity = HttpEntity(
      ContentType(`multipart/form-data`),
      Source.single(ByteString("Chunked")))),
    base.copy(entity = HttpEntity(
      ContentType(`multipart/form-data`),
      Source.single(ByteString("Chunked")))),
    base.copy(entity = HttpEntity(
      ContentType(`application/octet-stream`), "Binary".getBytes())))

  val data: immutable.Seq[(HttpRequest, HttpResponse)] = for {
    request <- requests
    response <- responses
  } yield (request, response)

  val req = Table(("requests", "responses"), data: _*)

  "full access" should {
    "check request/response combinations logging" in {
      forAll(req) {
        (request, response) =>
          new TestLoggingDirective(request, response).run()
      }
    }
    val bigEntity = base.copy(entity = HttpEntity((1 to 10000).mkString(",")))
    "log truncated response" in new TestLoggingDirective(Get(url), bigEntity) {
      override def maxBodySize = Some(1)

      lazy val hookedBodyLogger = new FakeLogging("FullAccessLoggingSpec-full-body-log")
      override protected lazy val fullBodyLog = Some(hookedBodyLogger.fakeLogger)
      hookedLogger.logOutput.forall(_.length < bigEntity.entity.contentLengthOption.get)
      hookedBodyLogger.logOutput.exists(_.length > bigEntity.entity.contentLengthOption.get)

    }
  }
}

object FullAccessLoggingSpec {
  class FakeLogging(name: String) {
    @volatile
    var logOutput: List[String] = List.empty[String]

    val fakeLogger: Logger = {
      val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      val ple = new EncoderBase[ILoggingEvent] {
        override def doEncode(event: ILoggingEvent): Unit =
          logOutput = logOutput :+ event.getMessage

        override def close(): Unit = {}
      }

      ple.setContext(lc)
      ple.start()
      val fileAppender = new ConsoleAppender[ILoggingEvent]
      fileAppender.setEncoder(ple)
      fileAppender.setContext(lc)
      fileAppender.start()

      val logger = LoggerFactory.getLogger(name).asInstanceOf[classic.Logger]
      logger.addAppender(fileAppender)
      logger.setLevel(classic.Level.DEBUG)
      logger.setAdditive(false)
      logger
    }

  }
}
