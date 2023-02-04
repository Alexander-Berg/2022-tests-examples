package common.clients.sraas

import common.protobuf.RegistryUtils._
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import org.apache.commons.io.IOUtils
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.StatusCode
import zio.{Has, Layer, ZLayer}
import zio.test.Assertion._
import zio.test._

import scala.jdk.CollectionConverters._

object SttpSraasClientSpec extends DefaultRunnableSpec {

  private def resourceAsByteArray(name: String) =
    IOUtils.toByteArray(SttpSraasClientSpec.getClass.getResourceAsStream(s"/$name"))

  private val sraasStub =
    AsyncHttpClientZioBackend.stub
      .whenRequestMatchesPartial {
        case r if r.uri.path.endsWith(List("full", "v0.0.1946")) =>
          Response.ok(resourceAsByteArray("full.bin"))
        case r if r.uri.path.endsWith(List("descriptors", "billing.howmuch.Matrix")) =>
          Response.ok(resourceAsByteArray("file_descriptor_set_response.bin"))
        case r if r.uri.path.endsWith(List("versions")) =>
          Response.ok(resourceAsByteArray("versions.bin"))
        case r if r.uri.path.endsWith(List("versions", "v0.0.467", "message-names")) =>
          Response.ok(resourceAsByteArray("message-names.bin"))
        case r if r.uri.path.endsWith(List("versions", "v0.0.1946", "message-names", "by-option")) =>
          Response.ok(resourceAsByteArray("message-names-by-option.bin"))
        case _ => Response("Not found", StatusCode.NotFound)
      }

  val config: Layer[Nothing, Has[Endpoint]] = Endpoint.testEndpointLayer

  val sraas = (config ++ Sttp.fromStub(sraasStub)) ++ ZLayer.succeed(vertisRegistry) >>> SttpSraasClient.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SttpSraasClient")(
      testM("parse FileDescriptorSetResponse response") {
        for {
          result <- SraasClient.getFileDescriptorSet("billing.howmuch.Matrix")
          version = result.map(_.getVersion)
        } yield assertTrue(version == Some("v0.0.5134"))
      },
      testM("handle not found response") {
        for {
          result <- SraasClient.getFileDescriptorSet("billing.howmuch.Matrix1")
        } yield assertTrue(result == None)
      },
      testM("handle getVersions") {
        for {
          result <- SraasClient.getVersions
          versions = result.getVersionsList.asScala
        } yield assertTrue(versions.size == 5220)
      },
      testM("handle getMessageNames") {
        for {
          result <- SraasClient.getMessageNames(Version.Custom("v0.0.467"))
          messageNames = result.getFullyQualifiedMessageNamesList.asScala
        } yield assertTrue(messageNames.size == 1433)
      },
      testM("handle getMessageNamesByOption") {
        for {
          result <- SraasClient.getMessageNamesWithOption("broker.config", Version.Custom("v0.0.1946"))
          messageNames = result.getFullyQualifiedMessageNamesList.asScala
          expected = Seq(
            "ru.auto.match_maker.MatchApplicationProcessedEvent",
            "vertis.telepony.TeleponyCall",
            "vertis.subscriptions.NotificationEvent"
          )
        } yield assert(messageNames)(hasSameElements(expected))
      }
    ).provideCustomLayerShared(sraas)
  }
}
