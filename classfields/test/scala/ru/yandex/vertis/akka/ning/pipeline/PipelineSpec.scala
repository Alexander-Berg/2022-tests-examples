package ru.yandex.vertis.akka.ning.pipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.akka.ning.client.HttpRequestSettings
import ru.yandex.vertis.util.ahc.HttpSettings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * @author @logab
  */
@RunWith(classOf[JUnitRunner])
class PipelineSpec
    extends WordSpecLike
        with Matchers
        with ScalaFutures
        with BeforeAndAfterEach
        with RequestBuilding {

  import PipelineSpec._

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Milliseconds))

  private implicit val as: ActorSystem = ActorSystem()
  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  trait Test {
    def responseFunction: HttpRequest => HttpResponse =
      r => HttpResponse(entity = r.method.toString())

    val server = Http().bindAndHandle(Flow.fromFunction(responseFunction), Host, Port).futureValue

    def httpSettings: HttpSettings = HttpSettings(100.millis, 1000.millis, 0, 10)

    val pipeline = new Pipeline(
      new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
          .setConnectTimeout(httpSettings.connectionTimeout.toMillis.toInt)
          .setRequestTimeout(httpSettings.requestTimeout.toMillis.toInt)
          .setMaxRequestRetry(httpSettings.numRetries)
          .setMaxConnections(httpSettings.maxConnections).build()))
  }

  "pipeline wrapper" should {
    val samplePath = s"http://$Host:$Port/$Path"
    "head" in new Test {
      private val httpResponse = pipeline(Head(samplePath)).futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.toStrict(100.millis).futureValue.data.utf8String shouldEqual ""
      server.unbind.futureValue
    }
    "get" in new Test {
      private val httpResponse = pipeline(Get(samplePath)).futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.toStrict(100.millis).futureValue.data.utf8String shouldEqual Get.method
          .toString()
      server.unbind.futureValue
    }
    "put" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => HttpResponse(
        entity = req.entity.toStrict(100.millis).futureValue.data.utf8String)
      private val httpResponse = pipeline(Put(samplePath, Body))
          .futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.toStrict(100.millis).futureValue.data.utf8String shouldEqual Body
      server.unbind.futureValue
    }
    "post" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => HttpResponse(
        entity = req.entity.toStrict(100.millis).futureValue.data.utf8String)
      private val httpResponse = pipeline(Post(samplePath, Body))
          .futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.toStrict(100.millis).futureValue.data.utf8String shouldEqual Body
      server.unbind.futureValue
    }
    "delete" in new Test {
      private val httpResponse = pipeline(Delete(samplePath)).futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      httpResponse.entity.toStrict(100.millis).futureValue.data.utf8String shouldEqual Delete.method
          .toString()
      server.unbind.futureValue
    }
    "fail to handle" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = _ => throw new UnsupportedOperationException()
      private val httpResponse = pipeline(Get(samplePath)).futureValue
      httpResponse.status shouldEqual StatusCodes.InternalServerError
      server.unbind.futureValue
    }
    "parse headers" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => HttpResponse(
        headers = req.headers)
      private val headers: List[HttpHeader] = List(
        akka.http.scaladsl.model.headers.Date(DateTime.now),
        akka.http.scaladsl.model.headers.Connection("close"))
      private val httpResponse = pipeline(Get(samplePath)
          .withHeaders(headers))
          .futureValue
      httpResponse.status shouldEqual StatusCodes.OK
      private val actual = httpResponse.headers.toSet
      private val expected = headers.toSet
      actual.intersect(expected) shouldEqual expected
      server.unbind.futureValue
    }

    "process form data" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => {
        HttpResponse(headers = req.headers, entity = req.entity)
      }

      private val bodyPart = Multipart.FormData.BodyPart.Strict(
        "file",
        HttpEntity(MediaTypes.`application/octet-stream`, ByteString(
          "test".getBytes)), // the chunk size here is currently critical for performance
        Map("filename" -> "testName"))
      val formData = FormData(Source.single(bodyPart))
      val entity: MessageEntity = Marshal(formData).to[RequestEntity].futureValue
      val request = Put(samplePath, entity)
      val result: HttpResponse = pipeline(request).futureValue
      result.status shouldEqual StatusCodes.OK
      private val actual: String = result.entity.toStrict(1.second).futureValue.data.utf8String
      private val expected: String = entity.toStrict(1.second).futureValue.data.utf8String
      actual.replaceAll("\r\n", "") shouldEqual expected.replaceAll("\r\n", "")
      server.unbind.futureValue
    }

    "handle request timeout" in new Test {
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => {
        Thread.sleep(2000)
        throw new UnsupportedOperationException()
      }
      override def httpSettings: HttpSettings = HttpSettings(100.millis, 100.millis, 0, 10)
      private val httpResponse = pipeline(Head(samplePath))
      httpResponse.failed.futureValue
      server.unbind.futureValue
    }

    "handle overridden request timeout" in new Test {
      import ru.yandex.vertis.akka.ning.client.HttpRequestSupport._
      override lazy val responseFunction: (HttpRequest) => HttpResponse = req => {
        Thread.sleep(2000)
        throw new UnsupportedOperationException()
      }
      override def httpSettings: HttpSettings = HttpSettings(100.millis, 3000.millis, 0, 10)
      private val httpResponse = pipeline(
        Head(samplePath).withSettings(HttpRequestSettings(requestTimeout = Some(100.millis))))
      httpResponse.failed.futureValue
      server.unbind.futureValue
    }
  }
}

object PipelineSpec {
  private val Path = "path"
  private val Host = "localhost"
  private val Port = 34328
  private val Body = "body"
}
