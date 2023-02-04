package vertis.pushnoy.services.xiva

import ru.auto.api.ResponseModel
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import vertis.pushnoy.conf.XivaClientConfig
import vertis.pushnoy.model.{Device, User}
import vertis.pushnoy.model.request.{DeviceInfo, PushMessageV1, RequestInfo, TokenInfo}
import vertis.pushnoy.model.request.params.PushTarget
import vertis.pushnoy.model.response.{PushCounterResponse, SecretSignResponse}
import vertis.pushnoy.services.xiva.TestXivaClient.InvalidId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** @author kusaeva
  */
class TestXivaClient extends XivaClient {

  private val success = SuccessResponse
    .newBuilder()
    .setStatus(ResponseStatus.SUCCESS)
    .build()

  private val successResponse = Future.successful(success)

  override def getSecretSign(
      user: User,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[SecretSignResponse] =
    Future.successful(SecretSignResponse("", "", Some("")))

  override def pushToDevice(
      device: Device,
      pushMessage: PushMessageV1,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[PushCounterResponse] =
    Future {
      if (device.id == InvalidId) {
        PushCounterResponse(0)
      } else {
        PushCounterResponse(1)
      }
    }

  override def pushToDevices(
      devices: Seq[Device],
      pushMessage: PushMessageV1,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[Seq[PushCounterResponse]] =
    Future.traverse(devices)(pushToDevice(_, pushMessage, clientConfig))

  override def pushToUser(
      user: User,
      pushMessage: PushMessageV1,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[PushCounterResponse] =
    Future.successful(PushCounterResponse(1))

  override def subscribe(
      device: Device,
      deviceInfo: Option[DeviceInfo],
      tokenInfo: TokenInfo,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[ResponseModel.SuccessResponse] = successResponse

  override def unsubscribe(
      device: Device,
      deviceInfo: TokenInfo,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[ResponseModel.SuccessResponse] =
    if (device.id == InvalidId) {
      Future.failed(new RuntimeException("Can't unsubscribe"))
    } else {
      successResponse
    }

  override def pushAndSocket(
      user: User,
      devices: Seq[Device],
      target: PushTarget,
      pushMessage: PushMessageV1,
      clientConfig: XivaClientConfig
    )(implicit ctx: RequestInfo): Future[Seq[PushCounterResponse]] =
    Future.successful(Seq(PushCounterResponse(1)))
}

object TestXivaClient {
  val InvalidId: String = "invalid"
}
