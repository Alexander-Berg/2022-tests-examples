package ru.yandex.vos2.autoru.services.blur

import java.io.File
import java.nio.file.{Files, Paths}
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.model.BlurCoordinates
import ru.yandex.vos2.autoru.services.blur.HttpLicensePlateBlur.generateDefaultNumberValue
import ru.yandex.vos2.util.HttpBlockingPool.TracedInstance
import ru.yandex.vos2.util.IO
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.util.Try

/**
  * Created by andrey on 3/17/17.
  */
@RunWith(classOf[JUnitRunner])
class LicensePlateBlurTest extends AnyFunSuite with MockHttpClientHelper with OptionValues {

  val coordinates = Array(Array(609L, 879L), Array(609L, 859L), Array(722L, 859L), Array(722L, 879L))

  private def blurClient(content: Array[Byte] = Array.empty): CloseableHttpClient = {
    val mockHttpClient = mock(classOf[CloseableHttpClient])
    val response = mockBinaryResponse(200, content)

    when(mockHttpClient.execute(any())).thenReturn(response)

    mockHttpClient
  }

  val recognizeResponse =
    """
      |{
      |  "ocr":
      |    [
      |      {
      |        "corners":
      |          [
      |            [
      |              609,
      |              879
      |            ],
      |            [
      |              609,
      |              859
      |            ],
      |            [
      |              722,
      |              859
      |            ],
      |            [
      |              722,
      |              879
      |            ]
      |          ],
      |        "confidence":0.7024750113,
      |        "text":"O009XE52",
      |        "width_ratio":0.1079881657
      |      }
      |    ]
      |}
      |""".stripMargin

  // base64 encode of string "image" = aW1hZ2U=
  val blurResponse =
    """
      |{
      |  "cbirdaemon":
      |    {
      |      "Overlay":"aW1hZ2U="
      |    }
      |}
      |""".stripMargin

  test("test blur") {
    implicit val t = Traced.empty

    val content: Array[Byte] = "image".getBytes
    val inFile = IO.newTempFile("autoru", "_photo_blur")

    val mdsUploader = new HttpLicensePlateBlur("http://example.com") {
      override protected val client: TracedInstance = new TracedInstance(blurClient(content = blurResponse.getBytes))

      override def recognizeNumber(file: File)(implicit trace: Traced): Seq[FileRecognizedNumberResult] =
        Seq(FileRecognizedNumberResult(generateDefaultNumberValue, Some(BlurCoordinates(coordinates))))
    }

    val outFile = mdsUploader.blur(inFile, None).file
    val fileContent = Files.readAllBytes(Paths.get(outFile.getAbsolutePath))
    assert(fileContent.sameElements(content))

    inFile.delete()
    outFile.delete()
  }

  test("recognize license number: success") {
    implicit val t = Traced.empty
    val content = recognizeResponse.getBytes
    val inFile = IO.newTempFile("autoru", "_photo_blur")

    val client = new HttpLicensePlateBlur("http://example.com") {
      override protected val client: TracedInstance = new TracedInstance(blurClient(content = content))
    }

    val result = client.recognizeNumber(inFile)
    assert(result.nonEmpty)
    assert(result.head.number.getConfidence.equals(0.7024750113))
    assert(result.head.number.getNumber.equals("O009XE52"))
    assert(result.head.number.getWidthPercent.equals(0.1079881657))
    assert(result.head.corners.get.coordinates.corresponds(coordinates)(_.sameElements(_)))

    inFile.delete()
  }

  val emptyListRecognizeResponse =
    """
      |{
      |  "ocr":
      |    [
      |    ]
      |}
    """.stripMargin

  test("recognize licence number: empty list") {
    implicit val t = Traced.empty
    val content = emptyListRecognizeResponse.getBytes
    val inFile = IO.newTempFile("autoru", "_photo_blur")

    val client = new HttpLicensePlateBlur("http://example.com") {
      override protected val client: TracedInstance = new TracedInstance(blurClient(content = content))
    }

    val result = client.recognizeNumber(inFile)
    assert(result.nonEmpty)
    assert(result.head.number.getConfidence.equals(0.0))
    assert(result.head.number.getNumber.equals(""))
    assert(result.head.number.getWidthPercent.equals(0.0))
    assert(result.head.corners.isEmpty)

    inFile.delete()
  }

  val emptyRecognizeResponse =
    """
      |{
      |}
    """.stripMargin

  test("recognize licence number: empty response") {
    implicit val t = Traced.empty
    val content = emptyRecognizeResponse.getBytes
    val inFile = IO.newTempFile("autoru", "_photo_blur")

    val client = new HttpLicensePlateBlur("http://example.com") {
      override protected val client: TracedInstance = new TracedInstance(blurClient(content = content))
    }

    val result = Try(client.recognizeNumber(inFile))

    assert(result.isFailure)
    assert(result.failed.get.isInstanceOf[NoSuchElementException])

    inFile.delete()
  }

}
