package ru.yandex.realty.componenttest.telepony

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import ru.yandex.realty.componenttest.http.ExternalHttpComponents.toTeleponyPath
import ru.yandex.realty.componenttest.http.ExternalHttpStub
import ru.yandex.realty.componenttest.http.ExternalHttpStubUtils.getOrMatchAll
import ru.yandex.realty.telepony.GetOrCreateResponse

trait TeleponyHttpStub extends ExternalHttpStub {

  import ru.yandex.realty.telepony.JsonFormats._

  def stubTeleponyGetOrCreateOk(objectId: String, source: String): Unit = {
    stubPostJsonResponse(
      toTeleponyPath(s"/api/2\\.x/${getOrMatchAll()}/redirect/getOrCreate/$objectId/"),
      status = StatusCodes.OK.intValue,
      response = GetOrCreateResponse(
        id = UUID.randomUUID().toString,
        objectId = objectId,
        tag = None,
        createTime = DateTime.now(),
        deadline = None,
        source = source,
        target = source,
        options = None
      )
    )
  }

  def stubTeleponyGetOrCreateError(objectId: String): Unit = {
    stubPostResponse(
      toTeleponyPath(s"/api/2\\.x/${getOrMatchAll()}/redirect/getOrCreate/$objectId/"),
      status = StatusCodes.InternalServerError.intValue,
      response = Array.empty[Byte],
      requestMatcher = None
    )
  }

  def stubTeleponyDeleteOk(
    objectId: String,
    redirectId: Option[String] = None
  ): Unit = {
    stubDeleteResponse(
      toTeleponyPath(s"/api/2\\.x/${getOrMatchAll()}/redirect/$objectId/${getOrMatchAll(redirectId)}")
    )
  }

  def stubTeleponyDeleteError(
    objectId: String,
    redirectId: Option[String] = None
  ): Unit = {
    stubDeleteResponse(
      toTeleponyPath(s"/api/2\\.x/${getOrMatchAll()}/redirect/$objectId/${getOrMatchAll(redirectId)}"),
      status = StatusCodes.InternalServerError.intValue
    )
  }

}
