package ru.auto.salesman.client.json

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.salesman.client.json.JsonExecutor.UnexpectedStatusException
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, RequestContext, UrlInterpolation}

import scala.concurrent.Future

class JsonExecutorImplSpec extends BaseSpec with IntegrationPatience {

  private val serverAddress = runServer {
    (get & path("slow")) {
      // simulate slow request handling
      Thread.sleep(1000)
      complete("{}")
    }
    (get & path("badRequest")) {
      complete(HttpResponse(StatusCodes.BadRequest))
    }
  }

  private val executor = new JsonExecutorImpl(serverAddress.toString)

  implicit private val resolver = new Resolver[String] {

    def context(path: String): String = path

    override def headers(path: String): Map[String, String] = Map()
  }

  implicit private val rc: RequestContext = AutomatedContext("test")

  "Json executor" should {

    "handle lots of parallel slow requests without timeouts" in {
      Future.sequence {
        1.to(10).map { _ =>
          Future(executor.get("/slow")).flatMap(Future.fromTry)
        }
      }.futureValue
    }

    "raise UnexpectedStatusException for unexpected statuses" in {
      Future(
        executor.get("/badRequest")
      ).futureValue.failure.exception shouldBe an[UnexpectedStatusException]
    }

    "escape special characters in url" in {
      val featureId = "promo:prefix_<ljhuyyhj:162e2bc5457ac480"
      val featureCount = 1000

      val path = url"/test/a:b/$featureId/$featureCount"

      val expectedUrl =
        "/test/a:b/promo%3Aprefix_%3Cljhuyyhj%3A162e2bc5457ac480/1000"

      executor.getUrl(
        path
      ) shouldBe (serverAddress.toString + "/" + expectedUrl)
    }

    "do simple escaping in url" in {
      val userId = 123

      val path = url"/user/$userId/some/endpoint"

      val expectedUrl =
        "/user/123/some/endpoint"

      executor.getUrl(
        path
      ) shouldBe (serverAddress.toString + "/" + expectedUrl)
    }

  }
}
