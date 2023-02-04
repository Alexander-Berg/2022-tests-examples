package ru.yandex.vertis.moisha.impl.autoru.example

import java.io.IOException

import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpResponse, HttpStatus}

/**
  * Examples of Moisha interaction.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
package object httpclient {

  /**
    * Executes Http request with Apache HttpClient.
    *
    * @param requestContent json representation of Moisha request
    */
  def executeWithApache[T](requestContent: String, responseHandler: HttpResponse => T): T = {
    val entity = EntityBuilder
      .create()
      .setContentType(ContentType.APPLICATION_JSON)
      .setText(requestContent)
      .build()

    val post = RequestBuilder
      .post()
      .setUri(MoishaTestingUrl)
      .setEntity(entity)
      .build()

    val client = HttpClientBuilder.create().build()

    var response: HttpResponse = null
    try {
      response = client.execute(post)

      if (response == null || response.getStatusLine == null)
        throw new IOException("Response or response status is null")

      response.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK =>
          responseHandler(response)
        case HttpStatus.SC_NOT_FOUND =>
          throw new IllegalArgumentException("Requested resource not found")
        case HttpStatus.SC_BAD_REQUEST =>
          throw new IllegalArgumentException("Malformed request")
        case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
          throw new IllegalArgumentException("Internal server error")
        case other =>
          throw new IOException(s"Unexpected response: ${response.getStatusLine.toString}")
      }
    } finally {
      if (response != null) EntityUtils.consumeQuietly(response.getEntity)
    }
  }
}
