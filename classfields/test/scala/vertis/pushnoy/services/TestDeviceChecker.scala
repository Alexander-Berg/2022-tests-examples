package vertis.pushnoy.services

import vertis.pushnoy.model.DeviceFullInfo
import vertis.pushnoy.model.request.{PushMessageV1, RequestInfo}
import vertis.pushnoy.services.xiva.{DeclinePushReason, DeviceChecker}

/** @author kusaeva
  */
class TestDeviceChecker extends DeviceChecker {

  override def check(
      fullInfo: DeviceFullInfo,
      pushMessage: PushMessageV1,
      subscription: Option[String]
    )(implicit ctx: RequestInfo): Either[DeclinePushReason, Unit] = Right(())
}
