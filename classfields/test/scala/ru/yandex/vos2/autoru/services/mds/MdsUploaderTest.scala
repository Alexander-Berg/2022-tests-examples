package ru.yandex.vos2.autoru.services.mds

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.services.tvm.FixedTicketWrapper
import ru.yandex.vos2.services.mds.MdsPhotoData
import ru.yandex.vos2.util.HttpBlockingPool.TracedInstance
import ru.yandex.vos2.util.RandomUtil
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration._
import ru.yandex.vertis.baker.components.http.client.tvm.TvmClientWrapper

/**
  * Created by andrey on 3/17/17.
  */
@RunWith(classOf[JUnitRunner])
class MdsUploaderTest extends AnyFunSuite with MockHttpClientHelper {

  private def mdsClient(groupId: Int = generateGroupId,
                        imageName: String = generateImageName,
                        content: Array[Byte] = Array.empty): CloseableHttpClient = {
    val mockHttpClient = mock(classOf[CloseableHttpClient])
    val postResponse = mockResponse(200, s"""{"group-id":$groupId, "imagename":"$imageName"}""")
    val getResponse = mockBinaryResponse(200, content)

    when(mockHttpClient.execute(any(classOf[HttpPost]))).thenReturn(postResponse)
    when(mockHttpClient.execute(any(classOf[HttpGet]))).thenReturn(getResponse)

    mockHttpClient
  }

  private def generateGroupId: Int = RandomUtil.nextInt(1000, 9000)

  private def generateImageName: String = {
    RandomUtil.nextHexString(32)
  }

  test("put") {
    implicit val t = Traced.empty
    val groupId = generateGroupId
    val imageName = generateImageName
    val ticket = RandomUtil.nextHexString(32)
    val tvm = FixedTicketWrapper(ticket)
    val mockedMdsClient: CloseableHttpClient = mdsClient(groupId, imageName)
    val mdsUploader = new HttpMdsUploader("http://example.com", "http://example.com", 7.days, tvm) {
      override protected val client: TracedInstance = new TracedInstance(mockedMdsClient)
    }

    val namespace = "test-ns"
    val res = mdsUploader.putFromArray(Array[Byte](1, 2, 3, 4, 5), namespace)
    assert(res == MdsPhotoData(namespace, s"$groupId-$imageName"))

    verify(mockedMdsClient).execute(argThat[HttpPost] { req =>
      req.getFirstHeader(TvmClientWrapper.ServiceTicketHeaderName).getValue == ticket
    })
  }

  test("get") {
    implicit val t: Traced = Traced.empty
    val content: Array[Byte] = "image".getBytes
    val ticket = RandomUtil.nextHexString(32)
    val tvm = FixedTicketWrapper(ticket)
    val mockedMdsClient: CloseableHttpClient = mdsClient(content = content)
    val mdsUploader = new HttpMdsUploader("http://example.com", "http://example.com", 7.days, tvm) {
      override protected val client: TracedInstance = new TracedInstance(mockedMdsClient)
    }

    val namespace = "test-ns"
    mdsUploader.actionWithOrigPhoto(MdsPhotoData(namespace, "xxx-yyy")) { file =>
      val fileContent: String = scala.io.Source.fromFile(file).mkString
      assert(fileContent.getBytes.sameElements(content))
    }

    verify(mockedMdsClient).execute(argThat[HttpGet] { req =>
      req.getFirstHeader(TvmClientWrapper.ServiceTicketHeaderName).getValue == ticket
    })
  }
}
