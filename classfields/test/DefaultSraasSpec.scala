package common.sraas.test

import common.clients.sraas.SttpSraasClient
import common.protobuf.RegistryUtils
import common.sraas.{DefaultSraas, Sraas}
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import org.apache.commons.io.IOUtils
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.{assertTrue, DefaultRunnableSpec}
import zio.{Has, Layer, ZLayer}

object DefaultSraasSpec extends DefaultRunnableSpec {

  private val getFileDescriptorSetStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    IOUtils.toByteArray(DefaultSraasSpec.getClass.getResourceAsStream("/file_descriptor_set_response.bin"))
  }

  val sttpConfig: Layer[Nothing, Has[Endpoint]] = Endpoint.testEndpointLayer

  override def spec = {
    suite("DefaultSraas")(
      testM("get descriptor by its name") {
        for {
          result <- Sraas.getDescriptor("billing.howmuch.Matrix")
        } yield {
          assertTrue(result.protoMessageName == result.descriptor.getFullName)
        }
      },
      testM("parse descriptor extensions") {
        for {
          result <- Sraas.getDescriptor("billing.howmuch.Matrix")
          idField = Option(result.descriptor.findFieldByName("matrix_id"))
        } yield {
          assertTrue(idField.exists(_.getOptions.getUnknownFields.asMap().isEmpty))
        }
      }
    ).provideCustomLayerShared {
      val sraasClient = sttpConfig ++ Sttp.fromStub(getFileDescriptorSetStub) ++ ZLayer.succeed(
        RegistryUtils.vertisRegistry
      ) >>> SttpSraasClient.live
      sraasClient >>> DefaultSraas.live
    }
  }
}
