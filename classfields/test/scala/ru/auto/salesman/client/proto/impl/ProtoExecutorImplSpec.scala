package ru.auto.salesman.client.proto.impl

import akka.http.scaladsl.model.StatusCodes.{Accepted, Conflict}
import akka.http.scaladsl.server.Directives._
import org.apache.http.entity.{ByteArrayEntity, ContentType}
import org.apache.http.util.EntityUtils
import ru.auto.salesman.client.proto.ProtoExecutorException.ConflictException
import ru.auto.salesman.client.proto.Resolver
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

class ProtoExecutorImplSpec extends BaseSpec {

  private val slowRoute =
    (get & path("slow")) {
      // simulate slow request handling
      Thread.sleep(1000)
      complete("")
    }

  private val conflictRoute =
    (get & path("conflict")) {
      complete(Conflict)
    }

  private val resultNotReadyRoute =
    (get & path("resultNotReady")) {
      complete(Accepted -> "NOT_READY")
    }

  private val serverAddress =
    runServer(concat(slowRoute, conflictRoute, resultNotReadyRoute))

  private val executor = new ProtoExecutorImpl(serverAddress.toString)

  implicit private val resolver = new Resolver[String] {

    def context(path: String): String = path

    override def headers(path: String): Map[String, String] = Map()
  }

  "Proto executor" should {

    "handle lots of parallel slow requests without timeouts" in {
      ZIO.collectAllPar {
        1.to(10).map { _ =>
          executor.get("/slow")
        }
      }.success
    }

    "return ConflictException on 409 status code" in {
      executor.get("/conflict").failure.exception shouldBe a[ConflictException]
    }

    "return entity on 202 status code" in {
      executor.get("/resultNotReady").success.value.foreach { result =>
        EntityUtils.toString {
          new ByteArrayEntity(
            result,
            ContentType.create("application/protobuf")
          )
        } shouldBe "NOT_READY"
      }
    }
  }
}
