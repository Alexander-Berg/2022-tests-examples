package vertis.pushnoy.services.apple

import vertis.pushnoy.model.request.{DeviceInfo, RequestInfo}
import vertis.pushnoy.services.apple.AppleDeviceCheckClient.Token

import scala.concurrent.Future

/** @author kusaeva
  */
class TestAppleDeviceCheckClient extends AppleDeviceCheckClient {
  override def get(token: Token)(implicit ctx: RequestInfo): Future[Option[DeviceInfo.IosDeviceCheckBits]] = ???

  override def update(
      token: Token,
      iosDeviceCheckBits: DeviceInfo.IosDeviceCheckBits
    )(implicit ctx: RequestInfo): Future[Boolean] = ???
}
