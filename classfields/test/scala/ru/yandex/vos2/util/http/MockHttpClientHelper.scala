package ru.yandex.vos2.util.http

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.{Header, HttpEntity, HttpHost, StatusLine}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

/**
  * Created by andrey on 8/29/16.
  */
trait MockHttpClientHelper {

  def mockHttpClient(status: Int = 200, body: String = "", throwException: Boolean = false): CloseableHttpClient = {
    val mockHttpClient = mock(classOf[CloseableHttpClient])

    if (throwException) {
      val exception = new RuntimeException("Mock http client exception")
      when(mockHttpClient.execute(any())).thenThrow(exception)
      when(mockHttpClient.execute(any[HttpHost](), any())).thenThrow(exception)
    } else {
      val response = mockResponse(status, body)
      when(mockHttpClient.execute(any())).thenReturn(response)
      when(mockHttpClient.execute(any[HttpHost](), any())).thenReturn(response)
    }
    mockHttpClient
  }

  def mockResponse(status: Int, body: String): CloseableHttpResponse = {
    val mockResponse = mock(classOf[CloseableHttpResponse])
    val mockStatusLine = mock(classOf[StatusLine])
    val mockHttpEntity = mock(classOf[HttpEntity])
    when(mockResponse.getStatusLine).thenReturn(mockStatusLine)
    when(mockStatusLine.getStatusCode).thenReturn(status)
    when(mockResponse.getEntity).thenReturn(mockHttpEntity)
    when(mockHttpEntity.getContent).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
    mockResponse
  }

  def mockBinaryResponse(
    status: Int,
    body: Array[Byte],
    headers: Seq[(String, String)] = Seq.empty
  ): CloseableHttpResponse = {
    val mockResponse = mock(classOf[CloseableHttpResponse])
    val mockStatusLine = mock(classOf[StatusLine])
    val mockHttpEntity = mock(classOf[HttpEntity])
    when(mockResponse.getStatusLine).thenReturn(mockStatusLine)
    when(mockStatusLine.getStatusCode).thenReturn(status)
    when(mockResponse.getEntity).thenReturn(mockHttpEntity)
    when(mockHttpEntity.getContent).thenReturn(new ByteArrayInputStream(body))

    headers.foreach(header => {
      val mockHeader = mock(classOf[Header])
      when(mockHeader.getName).thenReturn(header._1)
      when(mockHeader.getValue).thenReturn(header._2)
      when(mockResponse.getHeaders(header._1)).thenReturn(Array(mockHeader))
    })
    mockResponse
  }
}
