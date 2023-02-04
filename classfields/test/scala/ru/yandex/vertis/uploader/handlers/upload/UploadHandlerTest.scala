package ru.yandex.vertis.uploader.handlers.upload

import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import org.scalatest.WordSpec
import org.apache.commons.io.IOUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.baker.util.api.directives.RequestDirectives._
import ru.yandex.vertis.uploader.model.ImageModel.ImageBounds
import ru.yandex.vertis.uploader.model.{CallbackResponse, Cluster, SignedData, StorageType}
import ru.yandex.vertis.uploader.services.UploadService
import ru.yandex.vertis.uploader.util.JwtUtils

import scala.concurrent.Future

class UploadHandlerTest extends WordSpec with ScalatestRouteTest with MockitoSupport {

  "UploadHandler" should {
    "handle file name prefix" in {
      val prefix = "red_moskvich"
      var capturedName = ""
      val sign =
        JwtUtils.sign(
          SignedData(
            "test",
            Cluster.Default,
            StorageType.MdsAvatars,
            None,
            None,
            ImageBounds(1, 1, 1, 1),
            "",
            None,
            None,
            Seq.empty,
            None,
            None
          ),
          None
        )
      val pictureName = "random_car.jpeg"

      val uploadService = mock[UploadService]
      when(uploadService.upload(?, ?, ?, ?)(?)).thenAnswer(args => {
        capturedName = args.getArgument[String](3)
        Future.successful(CallbackResponse(200, Array.empty, ContentTypes.`application/json`))
      })

      val route = wrapRequest {
        new UploadHandler(uploadService).route
      }

      Get(
        s"?name_prefix=$prefix&sign=$sign",
        FormData(BodyPart.Strict("file", fileEntity(s"pictures/$pictureName"), Map("filename" -> pictureName)))
      ) ~> route ~> check {
        assert(handled)
        assert(capturedName.startsWith(prefix))
        assert(capturedName.length == 32)
      }
    }
  }

  private def fileEntity(resource: String): HttpEntity.Strict = {
    val file = ClassLoader.getSystemResourceAsStream(resource)
    HttpEntity(MediaTypes.`application/octet-stream`, ByteString(IOUtils.toByteArray(file)))
  }
}
