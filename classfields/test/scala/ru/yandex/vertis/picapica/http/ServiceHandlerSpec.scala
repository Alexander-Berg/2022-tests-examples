package ru.yandex.vertis.picapica.http

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.monitoring.{HealthChecks, Metrics}
import ru.yandex.util.spray.logging.MeteredRequest
import ru.yandex.vertis.picapica.actor.AsyncRequestActor
import ru.yandex.vertis.picapica.actor.AsyncRequestActor.Response
import ru.yandex.vertis.picapica.actor.AsyncRequestActor.Result.ImagesResult
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.{Metadata, OfferStructureBatch, Request}
import ru.yandex.vertis.picapica.client.{ApiVersion, PicaPicaSchemaVersion}
import ru.yandex.vertis.picapica.misc.marshalling.DomainMarshallers
import ru.yandex.vertis.picapica.misc.marshalling.DomainMarshallers.log
import ru.yandex.vertis.picapica.model.AvatarsResponse.{AvatarsData, Timeout}
import ru.yandex.vertis.picapica.model.{Id, MetaReloadTask, PartitionId, StoredAvatarsData}
import spray.http.StatusCodes
import spray.httpx.marshalling.Marshaller

/**
  * @author evans
  */

import scala.concurrent.duration._

//scalastyle:off
@RunWith(classOf[JUnitRunner])
class ServiceHandlerSpec
    extends RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(100.millis)

  implicit def actorRefFactory: ActorSystem = system

  private val asyncProbe = new TestProbe(system)

  private val serviceHandler =
    TestActorRef {
      new ServiceHandler {
        override def asyncProcessor: ActorRef = asyncProbe.ref

        override def responseMarshaller: Marshaller[Iterable[(Id, StoredAvatarsData)]] =
          DomainMarshallers.documentMarshaller(ApiVersion.V3)

        override def partitionBase: Int = 1024

        override val requests: Iterable[MeteredRequest] = Iterable.empty

        override def metricRegistry: MetricRegistry = Metrics.defaultRegistry()

        override val apiVersion: ApiVersion = ApiVersion.V3
      }
    }

  "Service handler" should {
    val requestData = Request.newBuilder().addElement(
      Request.Element.newBuilder().setKey("123").addValue(
        Request.Value.newBuilder().setKey("hash").setSrcUrl("url1")
      )
    ).addElement(
      Request.Element.newBuilder().setKey("124").addValue(
        Request.Value.newBuilder().setKey("hash1").setSrcUrl("url2")
      )
    ).setVersion(PicaPicaSchemaVersion.REQUEST_FORMAT_VERSION)

    val metadata = Metadata.newBuilder().setIsFinished(true).setVersion(5).build()

    val responseData = ImagesResult(
      Map(Id("123", "hash") -> StoredAvatarsData(
        AvatarsData(2, "fffuuu", None), Map("version" -> "5"), Some(metadata)),
        Id("124", "hash1") -> StoredAvatarsData(Timeout, Map.empty, None)))

    "async v3" in {
      val request = Post(s"/async/2", requestData.build().toByteArray)
      request ~> serviceHandler.underlyingActor.route ~> check {
        asyncProbe.expectMsgPF() {
          case r: AsyncRequestActor.DownloadRequest =>
            asyncProbe reply Response(r, responseData)
            response.status should be(StatusCodes.OK)
            log.info("OK")
          case x => println(x)
        }
        response.status should be(StatusCodes.OK)
      }
    }

    "version param" should {
      "optional" in {
        Post(s"/async/2", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, None, _, _, _) =>
          }
        }
      }

      "provided" in {
        Post(s"/async/2?version=34", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, Some(34), _, _, _) =>
          }
        }
      }

      "ignore wrong request" in {
        Post(s"/async/2?version=WrOnG", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectNoMsg()
        }
      }
    }

    "existing param" should {
      "false by default" in {
        Post(s"/async/2", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, _, false, _, _) =>
          }
        }
      }

      "ignore wrong request" in {
        Post(s"/async/2?existing=100500", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectNoMsg()
        }
      }

      "can be true" in {
        Post(s"/async/2?existing=true", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, _, true, _, _) =>
          }
        }
      }
    }

    "priority param" should {
      "be optional" in {
        Post(s"/async/2", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, _, _, _, 0) =>
          }
        }
      }

      "pass to request handler" in {
        Post(s"/async/2?priority=3", requestData.build().toByteArray) ~> serviceHandler.underlyingActor.route ~> check {
          asyncProbe.expectMsgPF() {
            case AsyncRequestActor.DownloadRequest(_, _, _, _, _, _, 3) =>
          }
        }
      }

      "not allow many uniq values" in {
        Post(s"/async/2?priority=999", requestData.build().toByteArray) ~>
          serviceHandler.underlyingActor.route ~> check {
            response.status should be(StatusCodes.BadRequest)
          }

        Post(s"/async/2?priority=-1", requestData.build().toByteArray) ~>
          serviceHandler.underlyingActor.route ~> check {
          response.status should be(StatusCodes.BadRequest)
        }
      }
    }

    "reload meta" in {
      Post("/async/2/reload-meta", OfferStructureBatch.newBuilder()
        .setVersion(1)
        .addOffers(
          OfferStructureBatch.Offer.newBuilder().setId("foo")
            .addElements(OfferStructureBatch.Element
              .newBuilder().setId("bar"))
        )
        .build().toByteArray
      ) ~> serviceHandler.underlyingActor.route ~> check {
        asyncProbe.expectMsgPF() {
          case AsyncRequestActor.MetaReloadRequest(
          Seq(MetaReloadTask(Id("foo", "bar"))), PartitionId("2", 2)) =>
        }
      }
    }
  }
}

//scalastyle:on
