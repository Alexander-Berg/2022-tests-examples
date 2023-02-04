package ru.yandex.auto.vin.decoder.scheduler.local.utils.audatex

import akka.http.scaladsl.model.HttpMethods
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.scheduler.local.utils.audatex.PalmaProxy.AlreadyExistsException
import ru.yandex.auto.vin.decoder.utils.http._
import auto.carfax.common.utils.tracing.TraceUtils._
import ru.yandex.vertis.commons.http.client.HttpClient.{protoResponseFormat, DefaultResponseFormat}
import ru.yandex.vertis.commons.http.client.RemoteHttpService
import ru.yandex.vertis.palma.services.ProtoDictionaryApiModel._
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

class PalmaProxy(httpService: RemoteHttpService) {

  private val createOrUpdateRoute = httpService.newRoute(
    "create_or_update_dictionary_entity",
    HttpMethods.POST,
    "/api/v1/palma/create_or_update"
  )

  private val getRoute = httpService.newRoute(
    "get_dictionary_entity",
    HttpMethods.GET,
    "/api/v1/palma/get"
  )

  def create(request: CreateUpdateRequest)(implicit t: Traced): Future[CreateUpdateResponse] = {
    createOrUpdateRoute
      .newRequest[CreateUpdateResponse]()
      .traced
      .withAccept(ProtobufContentType)
      .addQueryParam("is_update", false)
      .setEntity(request)
      .handle200(protoResponseFormat[CreateUpdateResponse])
      .handle(409)(DefaultResponseFormat.andThen(_ => throw AlreadyExistsException))
      .execute()
  }

  def update(request: CreateUpdateRequest)(implicit t: Traced): Future[CreateUpdateResponse] = {
    createOrUpdateRoute
      .newRequest[CreateUpdateResponse]()
      .traced
      .withAccept(ProtobufContentType)
      .addQueryParam("is_update", true)
      .setEntity(request)
      .handle200(protoResponseFormat[CreateUpdateResponse])
      .execute()
  }

  def get(request: GetRequest)(implicit t: Traced): Future[GetResponse] = {
    getRoute
      .newRequest[GetResponse]()
      .traced
      .withAccept(ProtobufContentType)
      .setEntity(request)
      .handle200(protoResponseFormat[GetResponse])
      .execute()
  }
}

object PalmaProxy {
  case object AlreadyExistsException extends RuntimeException with NoStackTrace
}
