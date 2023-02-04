package vertis.pushnoy.api.v1.user

import ru.yandex.pushnoy.PushRequestModel
import ru.yandex.pushnoy.push_response_model.ListPushSendResponse
import vertis.pushnoy.api.v2.user.UserManager
import vertis.pushnoy.model.request.RequestInfo
import vertis.pushnoy.model.{ClientType, DeliveryParams, User}

import scala.concurrent.Future

/** @author kusaeva
  */
class TestUserManager extends UserManager {

  override def sendTemplate(
      clientType: ClientType,
      user: User,
      templateReq: PushRequestModel.SendPushTemplateRequest,
      delivery: DeliveryParams
    )(implicit req: RequestInfo): Future[ListPushSendResponse] = ???
}
