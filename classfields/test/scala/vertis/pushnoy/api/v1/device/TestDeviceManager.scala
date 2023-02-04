package vertis.pushnoy.api.v1.device

import ru.yandex.pushnoy.PushRequestModel
import ru.yandex.pushnoy.push_response_model.PushSendSuccessResponse
import ru.yandex.vertis.generators.ProducerProvider
import vertis.pushnoy.api.v2.device.DeviceManager
import vertis.pushnoy.gen.ModelGenerators.DeviceInfoGen
import vertis.pushnoy.model.exception.UnsupportedApplicationException
import vertis.pushnoy.model.request.RequestInfo
import vertis.pushnoy.model.template.Template
import vertis.pushnoy.model.{ClientType, DeliveryParams, Device}

import scala.concurrent.Future

/** @author kusaeva
  */
class TestDeviceManager extends DeviceManager with ProducerProvider {

  override def sendTemplate(
      clientType: ClientType,
      device: Device,
      template: Template,
      delivery: DeliveryParams
    )(implicit req: RequestInfo): Future[PushSendSuccessResponse] =
    Future.failed(UnsupportedApplicationException(DeviceInfoGen.next, template))

  override def sendTemplateRequest(
      clientType: ClientType,
      device: Device,
      templateReq: PushRequestModel.SendPushTemplateRequest,
      delivery: DeliveryParams
    )(implicit req: RequestInfo): Future[PushSendSuccessResponse] = ???

  override def send(
      clientType: ClientType,
      device: Device,
      javaRequest: PushRequestModel.PushRequest,
      delivery: DeliveryParams
    )(implicit req: RequestInfo): Future[PushSendSuccessResponse] =
    Future.successful(PushSendSuccessResponse())

}
